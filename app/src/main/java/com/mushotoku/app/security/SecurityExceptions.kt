/*
 * Mushotoku — a privacy-focused, offline productivity app.
 * Copyright (C) 2026 Tom Frischmuth
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.mushotoku.app.security

class WrongPassphraseException(cause: Throwable? = null) :
    Exception("Falsche Passphrase oder beschaedigte Schluesseldaten", cause)

class KeyInvalidatedException(cause: Throwable? = null) :
    Exception("Biometrie-Schluessel ungueltig geworden (neue Biometrie registriert?)", cause)

class WrongModeException(message: String) : IllegalStateException(message)
