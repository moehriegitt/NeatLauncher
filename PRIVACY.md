# Privacy Policy

NeatLauncher respects your privacy.  There is no data collection or
analysis or copy or sending or anything.  NeatLauncher tries to be as
boring and unsurprising as possible.

This file will always contain current information about privacy
policy, i.e., this file will be updated if the policy changes.

## Persistent Configuration Data Store

NeatLauncher uses standard Android mechanisms (SharedPreferences) to
make its configuration (pinning, app visibility on the drawer, theme
selection, weather configuration) persistent across app or device
restarts.  I.e., the configuration is stored in persistent memory like
the sdcard or internal device memory.

When NeatLauncher is removed from the device, the persistent
configuration data store is erased (automatically by Android).

Backups of the Android system may make copies of the persistent
configuration data.

To see what is stored in the persistent data store, and also to allow
preserving this data across app deinstallation/reinstallation, the
preferences can be exported and imported.  The export files are in
JSON format and mildly human readable, if you are interested.

## Persistent State Data Store

For weather information, to avoid querying the weather server too
frequently, state information is stored in a similar data store as
configuration.  This store behaves the same: sdcard, internal memory,
removed on deinstallation.

This data store only contains weather related information at the
moment, including location coordinates and query times.

## Contacts

Contact access requires permission for which Android devices ask the
user.  When denied, then NeatLauncher cannot access your contact list,
and it will disable the functionality until you re-enable it manually
again.

If you select to include your contacts into the search, then
NeatLauncher queries the list of ID and display name of all contacts,
in order to search the display names. It displays the display names
that match the search.  If you click on a contact, NeatLauncher uses
the ID to open that contact in your 'Contacts' app.

If you change the configuration of a contact (e.g., by pinning it or
by making it visible in the drawer list), the ID and display name will
be stored in the persistent configuration data store (on the sdcard or
internal storage of the Android device), in order to display and
identify the contact.  When putting the contact configuration back to
the default, then that info is removed from the persistent app
configuration data store again.

NeatLauncher does not otherwise process the list of contacts.

## Network Connections: Weather

If you enable weather forecasts, then NeatLauncher opens an Internet
connection to open-meteo.com to search for locations and get weather
information.

Weather is NeatLauncher's only use of network connections.

NeatLauncher sends the location coordinates to the weather server,
which will process them.

If you search for a new location, then NeatLauncher sends your search
string to the server on the Internet and receives the list of results.

While inactive (e.g., while the device is locked), NeatLauncher does
not query weather information.

## Location Services

Location services require permission for which Android devices ask the
user.  When denied, then NeatLauncher cannot access current location.

If you select 'Current Location' for weather, then NeatLauncher
queries the current location via the 'fused' location service of our
Android device, which may invoke an array of different mechanisms to
determine your location, including contacting auxiliary servers on the
Internet.

This is NeatLauncher's only use of location services.

The only use of the current location is to store it as part of
configuration data, and to send it to the weather server to query a
weather forecast.

## Links to The Internet

NeatLauncher contains several links into the Internet, e.g., to source
code, F-Droid, Licence information, etc.  Such links are all processed
outside of NeatLauncher, i.e., another app is launched (usually the
web browser) to open the links.
