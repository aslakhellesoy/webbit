Webbit on Mongrel2
==================

Once you have installed Mongrel2, configure it with:

    m2sh load -config src/main/mongrel2/webbit.conf 

This will create `config.sqlite` in the current directory. Now start it:

   m2sh start -host localhost

Build OMQ Java bindings
-----------------------

Build [jzmq](https://github.com/zeromq/jzmq) from source.

If you're on OS X, set your library path so that the JNI lib can be found:

    DYLD_LIBRARY_PATH=/usr/local/lib

If you later get a `Unable to load native library: libjava.jnilib`, set it like this:

    DYLD_LIBRARY_PATH=/System/Library/Frameworks//ApplicationServices.framework/Versions/A/Frameworks/ImageIO.framework/Resources:/usr/local/lib


