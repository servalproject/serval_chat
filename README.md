Serval Chat
-----------

UI Re-write of serval's android application. With a focus on making it easier to communicate via text messaging.
Using more modern design principles, and API support. While retaining as much backward compatibility as practical.

The focus so far has been on building something functional.
More effort has been spent on functionality than appearance. 


Status
------


You can;
- Create multiple local identities
- Post to your own broadcast message feed (the current name of your identity will be copied to the name of the feed)
- Connect to other users over Wi-Fi or bluetooth to syncrhonize content
- List other reachable identities
- See some details of each identity
- See a simple visual fingerprint as an avatar for every identity
- List every broadcast feed with names, currently in your local rhizome store
- List the messages of any broadcast feed
- Follow, Block and Un-Follow any broadcast feed
- List the messages of Followed feeds in the order they arrived
- Reply privately to the author of any feed
- List incoming private conversations
- List private conversation threads
- Be notified of incoming private messages


You can't (yet or ever?);
- Create multiple feeds for the same identity. You can create multiple identities, but nobody else can tell they are from the same person.
- Protect identities with a PIN
- Control which of your identities are usable / visible to others nearby
- Provide your own local nickname for any other broadcast feeds
- Be notified of incoming broadcast messages
- See delivery or last read markers in private messaging, the information exists but is not yet visible.
- Disable the app. If you are connected to wifi or bluetooth is enabled, the app will attempt to find other nearby users, 
  there is currently no off switch.


Known issues;
- An incoming private message doesn't include the public key required to link this message to a broadcast feed.
  If the author of the private message has published a broadcast feed, and the user opens the list of all feeds, or has followed this feed,
  or if the author has been visible in the Near By list,
  then the link between the feed and the private message should be discovered.
  Some internal changes will be required that will break compatibility with previous versions of Serval Mesh.

- Following a feed appends *all* of their previous messages to your activity. This also occurs if you Ignore a feed and Follow it again.

