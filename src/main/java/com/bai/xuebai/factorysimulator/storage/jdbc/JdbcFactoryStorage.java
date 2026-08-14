package com.bai.xuebai.factorysimulator.storage.jdbc;

import com.bai.xuebai.factorysimulator.FactorySimulator;
import com.bai.xuebai.factorysimulator.config.PluginConfig;
import com.bai.xuebai.factorysimulator.model.FactoryProfile;
import com.bai.xuebai.factorysimulator.model.PlacedMachine;
import com.bai.xuebai.factorysimulator.storage.FactoryStorage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JdbcFactoryStorage implements FactoryStorage {
    private final FactorySimulator plugin;
    private final PluginConfig config;
    private final Map<String, FactoryProfile> cache = new HashMap<String, FactoryProfile>();
    private Connection connection;
    private boolean sqlite;

    public JdbcFactoryStorage(FactorySimulator plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public void load() {
        try {
            this.sqlite = config.getStorageType().name().equalsIgnoreCase("SQLITE");
            if (sqlite) {
                connection = DriverManager.getConnection("jdbc:sqlite:" + new java.io.File(plugin.getDataFolder(), config.getSqliteFileName()).getAbsolutePath());
            } else {
                String url = "jdbc:mysql://" + config.getMysqlHost() + ":" + config.getMysqlPort() + "/" + config.getMysqlDatabase() + "?" + config.getMysqlParams();
                connection = DriverManager.getConnection(url, config.getMysqlUser(), config.getMysqlPassword());
            }
            createTables();
            loadProfiles();
        } catch (SQLException ex) {
            throw new IllegalStateException("无法初始化数据库存储", ex);
        }
    }

    private void loadProfiles() throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM factory_profiles")) {
            while (resultSet.next()) {
                FactoryProfile profile = fromResultSet(resultSet);
                cache.put(profile.getPlayerId(), profile);
            }
        }
    }

    @Override
    public void saveAll() {
        for (FactoryProfile profile : new ArrayList<FactoryProfile>(cache.values())) {
            save(profile);
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            try { connection.close(); } catch (SQLException ignored) {}
        }
    }

    @Override
    public FactoryProfile getOrCreate(UUID uuid, String playerName) {
        FactoryProfile profile = cache.get(uuid.toString());
        if (profile == null) {
            profile = findById(uuid.toString());
        }
        if (profile == null) {
            profile = create(uuid.toString(), playerName);
        }
        profile.setPlayerName(playerName);
        cache.put(uuid.toString(), profile);
        return profile;
    }

    @Override
    public FactoryProfile getById(String id) {
        FactoryProfile profile = cache.get(id);
        if (profile != null) {
            return profile;
        }
        return findById(id);
    }

    @Override
    public FactoryProfile getByName(String name) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM factory_profiles WHERE player_name = ? LIMIT 1")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    FactoryProfile profile = fromResultSet(rs);
                    cache.put(profile.getPlayerId(), profile);
                    return profile;
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("按名称读取玩家工厂失败: " + ex.getMessage());
        }
        return null;
    }

    @Override
    public Collection<FactoryProfile> getAll() {
        return Collections.unmodifiableCollection(cache.values());
    }

    @Override
    public void save(FactoryProfile profile) {
        if (profile == null || profile.getPlayerId() == null) {
            return;
        }
        String sql = "REPLACE INTO factory_profiles (player_id, player_name, world_name, factory_name, created, created_at, last_online_at, last_claim_at, level, plot_size, money, offline_stored_money, workers, machines, achievements, unlocked_machines, layout) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            fill(ps, profile);
            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().warning("保存玩家工厂数据失败: " + profile.getPlayerId() + " -> " + ex.getMessage());
        }
    }

    @Override
    public boolean exists(UUID uuid) {
        return getById(uuid.toString()) != null;
    }

    private void createTables() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS factory_profiles (player_id VARCHAR(64) PRIMARY KEY, player_name VARCHAR(32), world_name VARCHAR(64), factory_name VARCHAR(64), created TINYINT, created_at BIGINT, last_online_at BIGINT, last_claim_at BIGINT, level INT, plot_size INT, money DOUBLE, offline_stored_money DOUBLE, workers INT, machines INT, achievements TEXT, unlocked_machines TEXT, layout TEXT)");
            try { st.executeUpdate("ALTER TABLE factory_profiles ADD COLUMN factory_name VARCHAR(64)"); } catch (SQLException ignored) {}
            try { st.executeUpdate("ALTER TABLE factory_profiles ADD COLUMN created TINYINT"); } catch (SQLException ignored) {}
            try { st.executeUpdate("ALTER TABLE factory_profiles ADD COLUMN layout TEXT"); } catch (SQLException ignored) {}
        }
    }

    private FactoryProfile create(String id, String name) {
        FactoryProfile profile = new FactoryProfile();
        profile.setPlayerId(id);
        profile.setPlayerName(name);
        profile.setWorldName("fs_" + id.replace("-", ""));
        profile.setFactoryName(name + "的工厂");
        profile.setCreated(false);
        profile.setCreatedAt(System.currentTimeMillis());
        profile.setLastOnlineAt(System.currentTimeMillis());
        profile.setLastClaimAt(System.currentTimeMillis());
        profile.setLevel(1);
        profile.setPlotSize(config.getInitialPlotSize());
        profile.setMoney(config.getStartingMoney());
        profile.setOfflineStoredMoney(0D);
        profile.setWorkers(config.getStartingWorkers());
        profile.setMachines(0);
        profile.getUnlockedMachines().add("basic_miner");
        save(profile);
        return profile;
    }

    private FactoryProfile findById(String id) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM factory_profiles WHERE player_id = ? LIMIT 1")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    FactoryProfile profile = fromResultSet(rs);
                    cache.put(profile.getPlayerId(), profile);
                    return profile;
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("按ID读取玩家工厂失败: " + ex.getMessage());
        }
        return null;
    }

    private FactoryProfile fromResultSet(ResultSet rs) throws SQLException {
        FactoryProfile profile = new FactoryProfile();
        profile.setPlayerId(rs.getString("player_id"));
        profile.setPlayerName(rs.getString("player_name"));
        profile.setWorldName(rs.getString("world_name"));
        profile.setFactoryName(rs.getString("factory_name"));
        profile.setCreated(rs.getBoolean("created"));
        profile.setCreatedAt(rs.getLong("created_at"));
        profile.setLastOnlineAt(rs.getLong("last_online_at"));
        profile.setLastClaimAt(rs.getLong("last_claim_at"));
        profile.setLevel(rs.getInt("level"));
        profile.setPlotSize(rs.getInt("plot_size"));
        profile.setMoney(rs.getDouble("money"));
        profile.setOfflineStoredMoney(rs.getDouble("offline_stored_money"));
        profile.setWorkers(rs.getInt("workers"));
        profile.setMachines(rs.getInt("machines"));
        profile.getAchievements().addAll(splitSet(rs.getString("achievements")));
        profile.getUnlockedMachines().addAll(splitList(rs.getString("unlocked_machines")));
        decodeLayout(rs.getString("layout"), profile);
        return profile;
    }

    private void fill(PreparedStatement ps, FactoryProfile profile) throws SQLException {
        ps.setString(1, profile.getPlayerId());
        ps.setString(2, profile.getPlayerName());
        ps.setString(3, profile.getWorldName());
        ps.setString(4, profile.getFactoryName()); ps.setBoolean(5, profile.isCreated());
        ps.setLong(6, profile.getCreatedAt()); ps.setLong(7, profile.getLastOnlineAt()); ps.setLong(8, profile.getLastClaimAt());
        ps.setInt(9, profile.getLevel()); ps.setInt(10, profile.getPlotSize()); ps.setDouble(11, profile.getMoney());
        ps.setDouble(12, profile.getOfflineStoredMoney()); ps.setInt(13, profile.getWorkers()); ps.setInt(14, profile.getMachines());
        ps.setString(15, join(profile.getAchievements())); ps.setString(16, join(profile.getUnlockedMachines())); ps.setString(17, encodeLayout(profile));
    }

    private String encodeLayout(FactoryProfile profile) {
        StringBuilder result = new StringBuilder();
        for (PlacedMachine machine : profile.getLayout()) {
            if (result.length() > 0) result.append(';');
            result.append(machine.getType()).append('|').append(machine.getX()).append('|').append(machine.getY()).append('|').append(machine.getZ()).append('|').append(machine.getFacing()).append('|').append(machine.getLevel()).append('|').append(machine.getProgress()).append('|');
            boolean first = true;
            for (Map.Entry<String, Integer> entry : machine.getInventory().entrySet()) {
                if (!first) result.append(',');
                result.append(entry.getKey()).append(':').append(entry.getValue()); first = false;
            }
        }
        return result.toString();
    }

    private void decodeLayout(String value, FactoryProfile profile) {
        if (value == null || value.trim().isEmpty()) return;
        for (String raw : value.split(";")) {
            String[] parts = raw.split("\\|", -1);
            if (parts.length < 8) continue;
            try {
                PlacedMachine machine = new PlacedMachine(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), parts[4]);
                machine.setLevel(Integer.parseInt(parts[5])); machine.setProgress(Long.parseLong(parts[6]));
                if (!parts[7].isEmpty()) for (String item : parts[7].split(",")) {
                    String[] pair = item.split(":", 2);
                    if (pair.length == 2) machine.getInventory().put(pair[0], Integer.parseInt(pair[1]));
                }
                profile.getLayout().add(machine);
            } catch (NumberFormatException ignored) {}
        }
    }

    private String join(Collection<String> values) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String value : values) {
            if (!first) builder.append(',');
            builder.append(value);
            first = false;
        }
        return builder.toString();
    }

    private List<String> splitList(String value) {
        List<String> list = new ArrayList<String>();
        if (value == null || value.trim().isEmpty()) return list;
        for (String s : value.split(",")) list.add(s);
        return list;
    }

    private java.util.Set<String> splitSet(String value) {
        return new java.util.HashSet<String>(splitList(value));
    }
}