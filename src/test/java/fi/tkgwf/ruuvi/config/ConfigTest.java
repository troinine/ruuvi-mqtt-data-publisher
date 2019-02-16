package fi.tkgwf.ruuvi.config;

import fi.tkgwf.ruuvi.strategy.impl.DefaultDiscardingWithMotionSensitivityStrategy;
import fi.tkgwf.ruuvi.strategy.impl.DiscardUntilEnoughTimeHasElapsedStrategy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigTest {

    @BeforeEach
    void resetConfigBefore() {
        Config.reload();
    }

    @AfterAll
    static void resetConfigAfter() {
        Config.reload();
    }

    @Test
    void testDefaultStringValue() {
        assertEquals("ruuvi", Config.getInfluxUser());
    }

    @Test
    void testDefaultBooleanValue() {
        assertEquals(true, Config.isInfluxBatch());
    }

    @Test
    void testDefaultIntegerValue() {
        assertEquals(100, Config.getInfluxBatchMaxTimeMs());
    }

    @Test
    void testDefaultLongValue() {
        assertEquals(9900, Config.getMeasurementUpdateLimit());
    }

    @Test
    void testOverriddenStringValue() {
        assertEquals("testing", Config.getInfluxPassword());
    }

    @Test
    void testOverriddenIntegerValue() {
        assertEquals(1234, Config.getInfluxBatchMaxSize());
    }

    @Test
    void testOverriddenBooleanValue() {
        assertFalse(Config.isInfluxGzip());
    }

    @Test
    void testOverriddenDoubleValues() {
        assertEquals(Double.valueOf(0.06d), Config.getDefaultWithMotionSensitivityStrategyThreshold());
    }

    @Test
    void testOverriddenMacFilterList() {
        assertFalse(Config.isAllowedMAC("ABCDEF012345"));
        assertFalse(Config.isAllowedMAC("F1E2D3C4B5A6"));
        assertTrue(Config.isAllowedMAC("123000000456"));
    }

    @Test
    void testNameThatCanBeFound() {
        assertEquals("Some named tag", Config.getTagName("AB12CD34EF56"));
    }

    @Test
    void testNameThatCanNotBeFound() {
        assertNull(Config.getTagName("123456789012"));
    }

    @Test
    void testLimitingStrategyPerMac() {
        assertTrue(Config.getLimitingStrategy("ABCDEF012345") instanceof DiscardUntilEnoughTimeHasElapsedStrategy);
        assertTrue(Config.getLimitingStrategy("F1E2D3C4B5A6") instanceof DefaultDiscardingWithMotionSensitivityStrategy);

        assertNull(Config.getLimitingStrategy("unknown should get null"));
    }

    @Test
    void testRefreshingConfigOnTheFly() {
        // Assert the default value:
        assertEquals("ruuvi", Config.getInfluxUser());

        // Load in a new value:
        final Properties properties = new Properties();
        properties.put("influxUser", "screw");
        Config.readConfigFromProperties(properties);

        // Test that it worked:
        assertEquals("screw", Config.getInfluxUser());
    }

    @Test
    void testInfluxDbFieldFilter() {
        final Properties properties = new Properties();
        properties.put("storage.values", "whitelist");
        Config.readConfigFromProperties(properties);
        Predicate<String> predicate = Config.getAllowedInfluxDbFieldsPredicate();

        assertFalse(predicate.test("quux")); // No values in the whitelist
        assertFalse(predicate.test("temperature")); // No values in the whitelist

        properties.put("storage.values.list", "foo,bar,temperature,something");
        Config.readConfigFromProperties(properties);
        predicate = Config.getAllowedInfluxDbFieldsPredicate();

        assertFalse(predicate.test("quux")); // Not whitelisted
        assertTrue(predicate.test("temperature")); // Whitelisted

        properties.put("storage.values", "blacklist");
        Config.readConfigFromProperties(properties);
        predicate = Config.getAllowedInfluxDbFieldsPredicate();

        assertTrue(predicate.test("quux")); // Not blacklisted
        assertFalse(predicate.test("temperature")); // Blacklisted

        properties.put("storage.values", "raw");
        Config.readConfigFromProperties(properties);
        predicate = Config.getAllowedInfluxDbFieldsPredicate();

        assertFalse(predicate.test("quux")); // Does not exist
        assertTrue(predicate.test("temperature")); // Allowed
        assertFalse(predicate.test("dewPoint")); // Not allowed

        properties.put("storage.values", "extended");
        Config.readConfigFromProperties(properties);
        predicate = Config.getAllowedInfluxDbFieldsPredicate();

        assertTrue(predicate.test("quux")); // Allowed
        assertTrue(predicate.test("temperature")); // Allowed
        assertTrue(predicate.test("dewPoint")); // Allowed
    }
}
