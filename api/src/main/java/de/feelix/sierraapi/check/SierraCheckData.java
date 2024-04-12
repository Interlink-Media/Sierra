package de.feelix.sierraapi.check;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The SierraCheckData annotation is used to mark a class as a check with a specified CheckType.
 * <p>
 * Example usage:
 * ```
 * @SierraCheckData(checkType = CheckType.SPAM)
 * public class SpamCheck {
 *     // Class implementation
 * }
 * ```
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SierraCheckData {

    /**
     * Retrieves the check type of the current instance.
     *
     * @return The check type of the instance.
     */
    CheckType checkType();

}
