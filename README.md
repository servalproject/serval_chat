Serval Chat README
==================

Serval Chat is a text messaging and social media application for Android. Enabling communications where 
traditional telephony solutions are unavailable.

UI Re-write of serval's android application. With a focus on making it easier to communicate via text messaging.
Using more modern design principles, and API support. While retaining as much backward compatibility as practical.

The focus so far has been on building something functional.
More effort has been spent on functionality than appearance. 


Dependencies
------------

- Android SDK & platform matching the compileSdkVersion found in app/build.gradle
- Android NDK version 15 or above
- git & sh binaries in your path environment variable


Getting Started
---------------

Obtaining the source code should be as simple as;

    $ git clone https://github.com/servalproject/serval_chat
    $ cd serval_chat
    $ git submodule init
    $ git submodule update

The gradle build process needs to know the install location of the Android SDK & NDK.
If you open the project in Android Studio these locations will be writen to local.properties as follows;

    ndk.dir={PATH}/Sdk/ndk-bundle
    sdk.dir={PATH}/Sdk

Libsodium may not build itself for the required architectures automatically due to no changes in the repo, fix it by running the 4 following scripts from /app/src/main/jni/libsodium/;

    $ /app/src/main/jni/libsodium/dist-build/android-armv7-a.sh
    $ /app/src/main/jni/libsodium/dist-build/android-armv8-a.sh
    $ /app/src/main/jni/libsodium/dist-build/android-x86.sh
    $ /app/src/main/jni/libsodium/dist-build/android-x86_64.sh

Building a debug APK from the command line should then be as simple as the following;

    $ ./gradlew assembleDebug

Building a release APK (signed) from the command line (A popup will appear during the build to enter the keystore password and the key password);

    $ ./gradlew assembleRelease -Prelease.key.store="PATH TO KEYSTORE FILE" -Prelease.key.alias="KEY ALIAS"


Development is primarily performed on a linux OS. While it should be possible to compile the native code components on a windows system, 
no attempt has yet been made to achieve this, and limited support will be provided.

Guidelines for debugging a release build
----------------------------------------

When trying to debug a release build of the app, if the phone isn't rooted we can't look at the internal storage directory where files required by the serval daemon are stored (keyring, pid, ports...).
Here is a procedure to extract the data from the phone, to look at the state of the internal storage located at /data/data/org.servalproject/ or data/user/0/org.servalproject/ depending on your phone.

First connect the phone and make sure it is available via adb;

    $ adb devices
    $ List of devices attached
    $ XXXXXXXXXXXXXX	device

Then we use the adb backup function to extract a snapshot of the data, in our current working directory;

    $ adb backup -noapk org.servalproject

A prompt to confirm the backup is displayed on the phone, don't put a password and click Save.

Then we need to convert this compressed file to a tar archive and extract it in the current directory, using this method;

    $ dd if=backup.ab bs=24 skip=1|openssl zlib -d > archive.tar
    $ tar xvf archive.tar

Finally we can cd into the directory (/apps/org/servalproject/) that will match the one we had (or have) on the phone when the backup was made;

    $ cd ./apps/org.servalproject/


Assisted P2P software upgrades
------------------------------

Building an APK signed with a rhizome manifest requires building servald targetted for the host machine as per app/src/main/jni/serval-dna/INSTALL.md.

Configuration can be supplied in a number of different waus to specify how the manifest should be constructed.

~/.gradle/gradle.properties
    ServalChat.properties={path}/gradle.properties

.../gradle.properties
    serval.keyring={filename}
    ${buildType}.manifest.id={id}
    ${buildType}.manifest.secret={private key}
    ${buildType}.manifest.author={sid}
    ${buildType}.manifest.bk={bk}

Providing an id is required, with the bundle secret either supplied explicitly, or derived from a keyring / author / bk.
See app/src/main/jni/serval-dna/doc/REST-API-Rhizome.md for more information.


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
- See last read markers in private messaging, the information exists but is not yet visible.
- Disable the app. If you are connected to wifi or bluetooth is enabled, the app will attempt to find other nearby users, 
  there is currently no off switch.


Known issues;
- An incoming private message doesn't include the public key required to link this message to a broadcast feed.
  If the author of the private message has published a broadcast feed, and the user opens the list of all feeds, or has followed this feed,
  or if the author has been visible in the Near By list,
  then the link between the feed and the private message should be discovered.
  Some internal changes will be required that will break compatibility with previous versions of Serval Mesh.

- Following a feed appends *all* of their previous messages to your activity. This also occurs if you Ignore a feed and Follow it again.
  This could be solved by scanning your feed index for a previous ACK, or adding a different type of marker into the activity to allow reading old messages.

- Navigating around the application is clunky. There are far too many functions hiding behind menu items. 
  Instead we should implement a navigation drawer, and make this the primary means of switching between screens and identities.

- If we fail to create or open the Rhizome store, no meaningful error is displayed to the user.
