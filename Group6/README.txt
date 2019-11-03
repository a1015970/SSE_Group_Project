SSE 2019 Group Project
Individual Code for Chris Crouch - a1015970
Group 6 (CHRIS CROUCH, PEIZONG LYU, TIANFANG WANG, HE ZHANG, WULI ZUO)

To Compile:
> cd src
> javac TallyTest.java
> javac CreateEncryptedVotesFile.java
> cd ..

To Run:
> java -classpath src CreateEncryptedVotesFile
(this will create a file of 10000 dummy votes called EncryptedVoteRecord)
> java -classpath src TallyTest
(this will read the EncryptedVoteRecord, verify and decrypt it, and then perform a tally on the votes.)

