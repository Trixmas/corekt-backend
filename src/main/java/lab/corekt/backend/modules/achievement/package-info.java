/**
 * Data-driven achievement system inspired by Minecraft's advancement system.
 *
 * <p>Achievements are defined in the database as JSONB criteria and evaluated at runtime
 * via a {@link lab.corekt.backend.modules.achievement.trigger.TriggerContext} built by
 * the calling service. New achievements can be added without code changes.
 */
package lab.corekt.backend.modules.achievement;