package io.supabase.auth

// watchOS suspends apps too, but its lifecycle notifications live in
// WatchKit (WKApplication) rather than UIKit; left without lifecycle
// handling for now, matching the previous behavior on all Apple targets.
internal actual suspend fun Auth.setupApplePlatform() = initDone()
