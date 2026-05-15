package com.ifsvivek.nammahomestay.data.role

/**
 * Which "side" of the app a user is currently looking at. The same Firebase uid
 * can flip between modes freely — it's purely a local UI preference, persisted
 * via DataStore so it survives an app restart but doesn't follow the user across
 * devices.
 */
enum class UserMode {
    HOST,
    TRAVELLER,
}
