/*
 * In-Game Account Switcher is a mod for Minecraft that allows you to change your logged in account in-game, without restarting Minecraft.
 * Copyright (C) 2015-2022 The_Fireplace
 * Copyright (C) 2021-2023 VidTu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

package ru.vidtu.ias.config;

/**
 * Title text alignment.
 *
 * @author VidTu
 */
public enum TextAlign {
    /**
     * Text is left-aligned.
     */
    LEFT("ias.config.textAlign.left"),

    /**
     * Text is center-aligned.
     */
    CENTER("ias.config.textAlign.center"),

    /**
     * Text is right-aligned.
     */
    RIGHT("ias.config.textAlign.right");

    /**
     * Alignment translation key.
     */
    private final String key;

    /**
     * Creates a new alignment.
     *
     * @param key Alignment translation key
     */
    TextAlign(String key) {
        this.key = key;
    }

    /**
     * Gets the translation key.
     *
     * @return Translation key
     */
    @Override
    public String toString() {
        return this.key;
    }
}
