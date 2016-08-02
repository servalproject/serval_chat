Serval Chat
-----------

UI Re-write of serval's android application. With a focus on making it easier to communicate via text messaging.
Using more modern design principles, and API support. While retaining as much backward compatibility as possible.

The current focus is on building a quick and dirty mockup of the features we wish to support. 
With more effort spent on functionality than appearance.
While hooking up any features that already exist in the Serval Mesh (aka batphone) app.


Status
------

Networking
- TODO; On / Off / Status (whatever that means...)
- TODO; Migrate bluetooth support from batphone

Identity handling
- Create multiple identities
- Each identity should open in a separate android task (pre 5.0, max of 4 tasks)
- activating android task may display a spinner before back end has started, after process killed
- TODO, entry pin support

Outgoing Public Message Feed
- TODO

Private Message Inbox
- Shows each conversation with PK of each peer
- TODO; Display peer name from ID card
- TODO; Preview of last sent / received message?

Private Messages List
- Send and receive private messages, including batphone peers
- infinite scrolling, fetching old messages on demand
- restful newsince api to fetch new messages without refetching entire list
- per-identity unread message notification, launches correct task
- TODO; highlight undelivered & unread messages

Peer List
- displays routable peers and response to DNA name / number queries
- can be used to initiate a private conversation

Peer Details
- ugly display of sid / name / number from DNA queries
- TODO; Block, Ignore, Add contact ...

Block List
- TODO

Unsolicited Inbox
- TODO
