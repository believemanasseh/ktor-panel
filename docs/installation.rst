Installation Guide
===================

Welcome to Ktor Panel! This guide will help you set up the library in your project, whether you prefer the reliability of Maven Central or the cutting-edge releases from GitHub.

Supported Platforms
-------------------
Ktor Panel is designed for Ktor applications and supports the following platforms:

- JVM (Java 17+)
- Kotlin 2.1+
- Ktor 3.0+
- Exposed ORM (optional, for database interactions)
- Hibernate ORM (optional, for JPA support)
- MongoDB (optional, for NoSQL support)

Install via Maven Central
-------------------------
The recommended way to install Ktor Panel is via Maven Central. This ensures you get verified, stable releases.

**Gradle (Kotlin DSL):**

.. code:: kotlin

   dependencies {
       implementation("xyz.daimones:ktor-panel:0.1.0")
   }

**Maven:**

.. code:: xml

   <dependency>
       <groupId>xyz.daimones</groupId>
       <artifactId>ktor-panel</artifactId>
       <version>0.1.0</version>
   </dependency>

No additional repository configuration is needed; Maven Central is included by default.

Install from GitHub Release
---------------------------
If you want the latest features or experimental builds, you can download binaries directly from GitHub Releases.

1. Visit the `Releases <https://github.com/believemanasseh/ktor-panel/releases>`__ page.
2. Download the desired JAR file (e.g., ``ktor-panel-0.1.0.jar``).
3. Place the JAR in your project's ``libs/`` directory.
4. Add it as a local dependency:

.. code:: kotlin

   dependencies {
       implementation(files("libs/ktor-panel-0.1.0.jar"))
   }

Install from Source (Advanced)
------------------------------
For contributors or those who want to customise the library, you can build from source:

1. Clone the repository:

.. code:: bash

   git clone https://github.com/believemanasseh/ktor-panel.git
   cd ktor-panel

2. Build the project:

.. code:: bash

   ./gradlew build

3. Find the JAR in ``lib/build/libs/`` and add it to your project as shown above.

Verifying Release Artifacts
----------------------------

To ensure the integrity and authenticity of downloaded release files, each artifact is accompanied by a `.asc` signature file.

1. Download both the artifact (e.g., `ktor-panel-0.1.0.jar`) and its `.asc` signature.
2. Obtain the maintainer's public GPG key. The key ID and fingerprint are published below.
3. Import the public key from a keyserver:

   .. code-block:: bash

      gpg --keyserver keyserver.ubuntu.com --recv-keys <KEY_ID>

4. Verify the artifact:

   .. code-block:: bash

      gpg --verify ktor-panel-0.1.0.jar.asc ktor-panel-0.1.0.jar

If verification succeeds, the artifact is authentic and untampered.

Maintainer GPG Key Information
------------------------------

- Key ID: `331D4ECF`
- Fingerprint: `F871 DDEE 2ABD BE95 99B8  53D7 8F3C E4B8 331D 4ECF`
- Public key: Available at `https://github.com/believemanasseh/ktor-panel/keys` or via keyserver.

Verifying Installation
----------------------
After installation, verify by importing the main classes in your code:

.. code:: kotlin

   import xyz.daimones.ktor.panel.Admin

If your IDE recognises the import, you’re ready to go!

Troubleshooting
---------------
- **Dependency not found?** Double-check the version and group/artifact IDs.
- **Build issues?** Ensure you’re using a compatible JDK (Java 17+ recommended).
- **Manual JAR install:** Make sure the JAR is in your ``libs/`` directory and the path is correct.
- **GPG verification failed?** Ensure you have the correct public key and that the artifact hasn't been tampered with.