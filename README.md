# Kotlin Liquibase

[![Build Status](https://travis-ci.org/redundent/liquibase-kotlin-dsl.svg?branch=master)](https://travis-ci.org/redundent/liquibase-kotlin-dsl)

A pluggable parser for [Liquibase](http://liquibase.org) that allows the
creation of changelogs in a Kotlin DSL, rather than hurtful XML. If this DSL
isn't reason enough to adopt Liquibase, then there is no hope for you.  This
project is HEAVILY based off of Groovy Liquibase which was started by Tim Berglund, and is currently maintained by Steve
Saliman.

## News

##### Additions to the XML format:
* In general, boolean attributes can be specified as either strings or booleans.
  For example, `changeSet(runAlways = "true")` can also be written as
  `changeSet(runAlways = true)`.
* The Kotlin DSL supports a simplified means of passing arguments to the
  `executeCommand change`.  Instead of:

```kotlin
execute {
  arg(value = "somevalue")
}
```
You can use this the simpler form:
```kotlin
execute {
  arg("somevalue")
}
```
* The `sql` change does not require a closure for the actual SQL.  You can
  just pass the string like this: `sql("select some_stuff from some_table")`
  If you want to use the `comments` element of a `sql` change, you need
  to use the closure form, and the comment must be in the closure BEFORE the
  SQL, like this:

```kotlin
sql {
  comment("we should not have added this...")
  -"delete from my_table"
}
```
* The  `stop` change can take a message as an argument as well as an
  attribute.  In other words, `stop("message")` works as well as the more
  XMLish `stop(message = "message")`
* A `customPrecondition`  can take parameters.  the XMLish way to pass them
  is with `param(name = "myParam", value = "myValue")` statements in the
  customPrecondition"s closure.  In the Kotlin DSL, you can also have
   `myParam("myValue")`
* The `validChecksum` element of a change set is not well documented.
  Basically you can use this when changeSet's current checksum will not match
  what is stored in the database. This might happen if you, for example want to
  reformat a changeSet to add white space.  This doesn't change the
  functionality of the changeset, but it will cause Liquibase to generate new
  checksums for it.  The `validateChecksum` element tells Liquibase to
  consider the checksums in the `validChecksum` element to be valid, even
  if it doesn't match what is in the database.
* The Liquibase documentation tells you how to set a property for a
  databaseChangeLog by using the `property` element.  What it doesn't tell
  you is that you can also set properties by loading a property file.  To do
  this, you can have `property(file = "my_file.properties")` in the closure
  for the databaseChangeLog.
* Liquibase has an `includeAll` element in the databaseChangeLog that
  includes all the files in the given directory.  The Kotlin DSL implementation
  only includes kotlin files, and it makes sure they are included in
  alphabetical order.  This is really handy for keeping changes in a different
  file for each release.  As long as the file names are named with the release
  numbers in mind, Liquibase will apply changes in the correct order.
* Remember, the Kotlin DSL is basically just Kotlin closures, so you can use
  kotlin code to do things you could never do in XML, such as this:

```kotlin
sql { """
  insert into some_table(data_column, date_inserted)
  values("some_data", "${Date().toString()}")
"""
}
```

##### Items that were left out of the XML documentation
* The `createIndex` and `dropIndex` changes have an undocumented
  `associatedWith` attribute.  From an old Liquibase forum, it appears to be
   an attempt to solve the problem that occurs because some databases
   automatically create indexes on primary keys and foreign keys, and others
   don't.  The idea is that you would have a change to create the primary key or
   foreign key, and another to create the index for it.  The index change would
   use the ```associatedWith``` attribute to let Liquibase know that this index
   will already exist for some databases so that Liquibase can skip the change
   if we are in one of those databases.  The Liquibase authors do say it is
   experimental, so use at your own risk...
* The `executeCommand` change has an undocumented `os` attribute.  The
  `os` attribute is a string with  a list of operating systems under which
  the command should execute.  If present, the ```os.name``` system property
  will be checked against this list, and the command will only run if the
  operating system is in the list.
* The `column` element has some undocumented attributes that are pretty
  significant.  They include:
    - `valueSequenceNext`, `valueSequenceCurrent`, and
      `defaultValueSequenceNext`, which appear to link values for a column
      to database sequences.

    - A column can be set auto-number if it the ```autoIncrement``` attribute is
      set to true, but did you know that you can also control the starting
      number and the increment interval with the ```startWith``` and
      ```incrementBy``` attributes?
* The ```constraints``` element also has some hidden gems:
    - Some databases automatically create indexes for primary keys. The
      ```primaryKeyTablespace``` can be used to control the tablespace.
    - There is also a ```checkConstraint``` attribute, that appears to be
      useful for defining a check constraint, but I could not determine the
      proper syntax for it yet.  For now, it may be best to stick to custom
      ```sql``` changes to define check constraints.
* The ```createSequence``` change has an ```cacheSize``` attribute that sets
  how many numbers of the sequence will be fetched into memory for each query
  that accesses the sequence.
* The documentation for version 3.1.1 of Liquibase mentions the new
  ```beforeColumn```, ```afterColumn```, and ```position``` attributes that you
  can put on a ```column``` statement to control where a new column is placed in
  an existing table.  What 1.2the documentation leaves out is that these attributes
  don't work :-)
* Version 3.4.0 of Liquibase introduced two new attributes to the 
  ```includeAll``` element of a databaseChangeLog, both of which are
  undocumented.  The first one is the ```errorIfMissingOrEmpty``` attribute.
  It defaults to ```true```, but if it is set to ```false```, Liquibase will
  ignore errors caused by invalid or empty directories and move on.  The second
  one is the ```resourceFilter``` attribute.  A resourceFilter is the name of a
  class that implements ```liquibase.changelog.IncludeAllFilter``` interface, 
  which allows developers to implement sophisticated logic to decide what files
  from a directory should be included (in addition to the *.kts filter that
  the Kotlin DSL imposes). 
* Liquibase 3.4.0 added the undocumented ```forIndexCatalogName```,
  ```forIndexSchemaName```, and ```forIndexName``` attributes to the 
  ```addPrimaryKey``` and ```addUniqueConstraint``` changes.  These attributes
  allow you to specify the index that will be used to implement the primary key
   and unique constraint, respectively.
* Liquibase 3.4.0 added the undocumented ```cacheSize``` and ```willCycle``` 
  attributes to the ```alterSequence```  change. ```cacheSize``` sets how many 
  numbers of the sequence will be fetched into memory for each query that 
  accesses the sequence.  ```willCycle``` determines if the sequence should 
  start over when it reaches its maximum value.

## License
This code is released under the Apache Public License 2.0, just like Liquibase 2.0.
