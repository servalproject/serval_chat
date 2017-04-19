Serval Chat
-----------

UI Re-write of serval's android application. With a focus on making it easier to communicate via text messaging.
Using more modern design principles, and API support. While retaining as much backward compatibility as possible.

The current focus is on building a quick and dirty mockup of the features we wish to support. 
With more effort spent on functionality than appearance.
While hooking up any features that already exist in the Serval Mesh (aka batphone) app.


Status
------


You can;
- Create multiple local identities
- Post to your own broadcast message feed (the current name of your identity will be copied to the name of the feed)
- Connect to other users over Wi-Fi or bluetooth to syncrhonize content
- List other reachable identities
- See some details of each identity
- List every broadcast feed with names, currently in your local rhizome store
- List the messages of any broadcast feed
- Follow and Un-Follow any broadcast feed
- List the messages of Followed feeds in the order they arrived
- Reply privately to the author of any feed
- List incoming private conversations
- List private conversation threads
- Be notified of incoming private messages


You can't (yet or ever?);
- Create multiple feeds for the same identity. You can create multiple identities, but nobody else can tell they are from the same person.
- Protect identities with a PIN
- Control which of your identities are usable / visible to others nearby
- Block, follow or provide your own local name for any broadcast feeds
- Be notified of incoming broadcast messages
- See delivery or last read markers
- Disable the app. If you are connected to wifi or bluetooth is enabled, the app will attempt to find other nearby users, there is no off switch.
- Reliably match incoming private messages with any broadcast message feed from the same identity. This matching may occur if the peer is online, but isn't reliable.
