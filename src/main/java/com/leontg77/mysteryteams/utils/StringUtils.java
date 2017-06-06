package com.leontg77.mysteryteams.utils;

import org.apache.commons.lang.Validate;

/**
 * String utilities class.
 * 
 * @author LeonTG77
 */
public class StringUtils {

    /**
     * Fix the given text with making the first letter capitalised and the rest not.
     *
     * @param text the text fixing.
     * @param replaceUnderscore True to replace all _ with a space, false otherwise.
     * @return The new fixed text.
     */
    public static String fix(String text, boolean replaceUnderscore) {
        Validate.notNull(text, "Text cannot be null.");

        if (text.isEmpty()) {
            return text;
        }

        if (text.length() == 1) {
            return text.toUpperCase();
        }

        if (replaceUnderscore) {
            text = text.replace("_", " ");
        }

        String toReturn = "";

        for (String split : text.split(" ")) {
            if (split.isEmpty()) {
                toReturn = toReturn + " ";
                continue;
            }

            if (text.length() == 1) {
                toReturn = toReturn + text.toUpperCase() + " ";
                continue;
            }

            toReturn = toReturn + split.substring(0, 1).toUpperCase() + split.substring(1).toLowerCase() + " ";
        }

        return toReturn.trim();
    }

    /**
     * Remove all formatting colors from the given message.
     *
     * @param message The message to remove from.
     * @return The new message without the formatting colors.
     */
    static String removeFormatColors(String message) {
        Validate.notNull(message, "Message cannot be null.");

        String newMessage = message;

        newMessage = newMessage.replaceAll("§l", "");
        newMessage = newMessage.replaceAll("§o", "");
        newMessage = newMessage.replaceAll("§r", "§f");
        newMessage = newMessage.replaceAll("§m", "");
        newMessage = newMessage.replaceAll("§n", "");

        return newMessage;
    }
}