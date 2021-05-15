# Apogee, a Gemini server

Apogee is a server for the [Gemini protocol](https://gemini.circumlunar.space/), written in Kotlin.

## Features

Apogee currently
has support for the following features:

- Static file serving.
- Redirections.
- Client authentication.
- CGI and SCGI.

## Dependencies

Apogee runs on a JVM, so some implementation is required (it was developed and tested on 
[OpenJDK](https://openjdk.java.net/)). Java 8 is the minimum supported version.

Other than that, Apogee depends on the following external libraries:
- [Netty](https://netty.io/) to handle socket connections. 
- [TOML4J](https://github.com/mwanji/toml4j) to handle configuration file parsing.
- [kotlinx-cli](https://github.com/Kotlin/kotlinx-cli) to handle command line options.

Apogee has been developed and tested for Linux; Windows is not officially supported, though if you try to run it on
Windows and encounter a specific problem please feel free to file an issue.

## Installation

Currently, the easiest way to run Apogee is to clone the Git repository:

```commandline
git clone https://github.com/bunburya/apogee.git
```

Then, from the cloned directory, run:

```shell
./gradlew run --args="--config /path/to/config.toml"
```

Alternatively, you can build a shell script:

```shell
./gradlew build -x test
```

This builds Apogee without running the tests (some of which are integration tests that depend on an already running
instance). The shell script can be found in the `build/distributions` directory (in the `bin` subdirectory of the
directory you get when you extract the zip or tar archive). You can then initialise Apogee using the following command
(make sure the `apogee` shell script is in your shell path):

```shell
apogee --config /path/to/config.toml
```

## Configuration

Apogee is configured through a single [TOML](https://toml.io/en/) file. This section will discuss the main configuration
options. In the `src/main/resources` directory you will find an example config file, populated with default values
where appropriate.

**NOTE:** Configuration of certain features, such as redirection, is done via key-value pairs stored in tables,
where the keys are regex patterns against which the path is compared. The first pattern which matches the path will be
used to determine the appropriate value. Tables in TOML files are not guaranteed to be in any particular order.
Therefore, you should be careful to ensure that patterns are mutually exclusive, as there is no guarantee as to what
order they will be checked.

### Basic settings

These are settings to control the basic behaviour of Apogee.

|Option name |Description | Default value|
|---|---|---|
|PORT|The port on which Apogee will listen for requests.|1965|
|HOSTNAME|The name of the host at which Apogee will listen for requests.|localhost|
|CERT_FILE|The location of the file containing the server's SSL certificate, in PEM format.|cert.pem|
|KEY_FILE|The location of the file containing the server's SSL key, in PEM format.|key.pem|
|DOCUMENT_ROOT|The location of the directory containing the files to be served by Apogee.|/var/gemini/|
|ACCESS_FILE|Where to log access attempts.|null (logs to standard error)|
|LOG_FILE|Where to store general logs.|null (logs to standard error)|
|LOG_LEVEL|How severe a log message must be in order to be logged to LOG_FILE. This should correspond to the levels used by the [java.util.logging](https://docs.oracle.com/javase/7/docs/technotes/guides/logging/overview.html#a1.2) library, ie, it should be one of "SEVERE", "WARNING", "INFO", "CONFIG", "FINE", "FINER" or "FINEST".|WARNING|

### Static files

These settings control certain aspects of how Apogee serves static files.

|Option name |Description | Default value|
|---|---|---|
|GEMINI_EXT|The file extension that Apogee will interpret as indicating the "text/gemini" MIME type.|gmi|
|INDEX_FILE|The name of the file that Apogee will attempt to serve if a request corresponds to a directory. If no index file is present, Apogee will display a basic index which lists files in the directory.|index.gmi|
|DIR_SORT_METHOD|*(Not yet implemented)* Where Apogee auto-generates an index for a directory, how to sort the files in the directory.|NAME|

The `MIME_OVERRIDES` section allows you to customise the MIME type that is sent where a filename corresponds to a given
regular expression.

### Dynamic content

Apogee currently supports dynamic content through the CGI and SCGI protocols. It is intended to also support FastCGI in
the future.

The `CGI_TIMEOUT` option governs how many seconds Apogee should wait for a response from a process before returning an
error response. It defaults to 10.

The `CGI_PATHS` option should be an array of filesystem paths to directories. Where a request corresponds to a file in
one of these directories, Apogee will attempt to run that file as a script and return the standard output of that script
to the client.

The `SCGI_PATHS` section is a table mapping (relative) URL prefixes to filesystem paths to Unix domain sockets. Where a
request contains one of the specified prefixes, it will be encoded according to the SCGI protocol and written to the 
relevant socket. It will then listen on the socket and return any output to the client. In the future it is intended to
support network sockets in addition to Unix domain sockets.

Both CGI and SCGI scripts are responsible for sending their own headers (eg, `20 text/gemini\r\n`).

The following are the main variables passed to applications over CGI/SCGI:

|Variable name |Description |Protocol|
|---|---|---|
|GATEWAY_INTERFACE|The name of the gateway interface used, followed, if applicable, by a "/" and the version number, eg, `CGI/1.1` or `SCGI`.|CGI/SCGI|
|SERVER_NAME|The hostname of the server on which Apogee is running.|CGI/SCGI|
|SERVER_PORT|The port on which Apogee is listening.|CGI/SCGI|
|SERVER_PROTOCOL|The protocol the server is using - always "GEMINI".|CGI/SCGI|
|SERVER_SOFTWARE|The software running the server - always "APOGEE".|CGI/SCGI|
|REMOTE_ADDR|The IP address from which the request originated.|CGI/SCGI|
|SCRIPT_PATH|The part of the request path corresponding to the relevant script. For CGI, this is the part of the path that corresponds to the CGI script to be run; for SCGI, it corresponds to the prefix specified in the config file.|CGI/SCGI|
|PATH_INFO|The part of the request path after SCRIPT_PATH (excluding any query string).|CGI/SCGI|
|QUERY_STRING|You guessed it - the query string, if any, specified in the request.|CGI/SCGI|
|REQUEST_METHOD|Always an empty string, as Gemini does not support different request methods.|CGI/SCGI|
|TLS_CLIENT_HASH|If a client certificate is provided, the SHA256 hash of that certificate. Not present otherwise.|CGI/SCGI|
|TLS_CLIENT_ISSUER_DN|If a client certificate is provided, the distinguished name of the certificate issuer. Not present otherwise.|CGI/SCGI|
|TLS_CLIENT_SUBJECT_DN|If a client certificate is provided, the distinguished name of the certificate subject. Not present otherwise.|CGI/SCGI|
|AUTH_TYPE|If a client certificate is provided, "Certificate". Not present otherwise.|CGI/SCGI|
|SCGI|Always "1", as required by the SCGI protocol.|SCGI|
|CONTENT_LENGTH|The length of the request body - this will always be 0, as Gemini does not permit request bodies.|SCGI|

### Redirects

You can tell Apogee to redirect certain paths using the `TEMP_REDIRECTS` and `PERM_REDIRECTS` sections of the config
file. Each entry in these sections should map a regular expression, against which a path will be checked, to the path to
redirect to. Requests matched against entries in the `TEMP_REDIRECTS` section will receive a temporary redirect
response (status code 30). Requests matched against entries in the `PERM_REDIRECTS` section will receive a permanent
redirect response (status code 31).

### Client authorisation

Apogee has support for client authentication using client-side SSL certificates. The `CLIENT_CERT_ZONES` section should
be a table mapping regular expressions to lists of supported client certificate SHA256 fingerprints. Client
authentication is done immediately after the request is received by the server, so redirections, dynamic content, etc,
will not be handled unless a request is authenticated (if necessary).

## Obligatory security warning

Remember that Apogee is still a work in progress and should not be considered to be completely secure. Use at your own risk. It is your responsibility to ensure that your server, and any scripts called by it, are secure. If you do discover any vulnerabilities or other issues with Apogee, please file an issue.