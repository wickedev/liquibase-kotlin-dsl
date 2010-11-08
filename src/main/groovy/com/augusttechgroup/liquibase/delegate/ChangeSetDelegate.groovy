//
// Groovy Liquibase ChangeLog
//
// Copyright (C) 2010 Tim Berglund
// http://augusttechgroup.com
// Littleton, CO
//
// Licensed under the GNU Lesser General Public License v2.1
//

package com.augusttechgroup.liquibase.delegate

import liquibase.change.core.AddColumnChange
import liquibase.change.core.RenameColumnChange
import liquibase.change.core.DropColumnChange
import liquibase.change.core.AlterSequenceChange
import liquibase.change.core.CreateTableChange
import liquibase.change.core.RenameTableChange
import liquibase.change.core.DropTableChange
import liquibase.change.core.CreateViewChange
import liquibase.change.core.RenameViewChange
import liquibase.change.core.DropViewChange
import liquibase.change.core.MergeColumnChange
import liquibase.change.core.CreateProcedureChange
import liquibase.change.core.AddLookupTableChange
import liquibase.change.core.AddNotNullConstraintChange


class ChangeSetDelegate {
  def changeSet


  void comment(String text) {
    changeSet.comments = text
  }


  void preConditions(Closure closure) {

  }


  //TODO Verify that this works. Don't fully understand addValidCheckSum() yet...
  void validCheckSum(String checksum) {
    println "ADDING ${checksum} to ${changeSet}"
    changeSet.addValidCheckSum(checksum)
  }


  void rollback(String sql) {
    changeSet.addRollBackSQL(sql)
  }


  void rollback(Closure closure) {
    changeSet.addRollBackSQL(closure.call().toString())
  }

  
  void rollback(Map params) {
    //TODO implement after changeSet processing is substantially implemented (testing requires it)
  }


  void addColumn(Map params, Closure closure) {
    def change = makeColumnarChangeFromMap(AddColumnChange, closure, params, ['schemaName', 'tableName'])
    changeSet.addChange(change)
  }


  void renameColumn(Map params) {
    addMapBasedChange(RenameColumnChange, params, ['schemaName', 'tableName', 'oldColumnName', 'newColumnName', 'columnDataType'])
  }


  void modifyColumn(Map params, Closure closure) {
    //TODO Figure out how the heck modifyColumn works.
  }


  void dropColumn(Map params) {
    addMapBasedChange(DropColumnChange, params, ['schemaName', 'tableName', 'columnName'])
  }


  void alterSequence(Map params) {
    addMapBasedChange(AlterSequenceChange, params, ['sequenceName', 'incrementBy'])
  }


  void createTable(Map params, Closure closure) {
    def change = makeColumnarChangeFromMap(CreateTableChange, closure, params, ['schemaName', 'tablespace', 'tableName', 'remarks'])
    changeSet.addChange(change)
  }


  void renameTable(Map params) {
    addMapBasedChange(RenameTableChange, params, ['schemaName', 'oldTableName', 'newTableName'])
  }


  void dropTable(Map params) {
    addMapBasedChange(DropTableChange, params, ['schemaName', 'tableName'])
  }
  

  void createView(Map params, Closure closure) {
    def change = makeChangeFromMap(CreateViewChange, params, ['schemaName', 'viewName', 'replaceIfExists'])
    change.selectQuery = closure.call()
    changeSet.addChange(change)
  }


  void renameView(Map params) {
    addMapBasedChange(RenameViewChange, params, ['schemaName', 'oldViewName', 'newViewName'])
  }


  void dropView(Map params) {
    addMapBasedChange(DropViewChange, params, ['schemaName', 'viewName'])
  }


  void mergeColumns(Map params) {
    addMapBasedChange(MergeColumnChange, params, ['schemaName', 'tableName', 'column1Name', 'column2Name', 'finalColumnName', 'finalColumnType', 'joinString'])
  }


  void createStoredProcedure(String storedProc) {
    def change = new CreateProcedureChange()
    change.procedureBody = storedProc
    changeSet.addChange(change)
  }

  
  void addLookupTable(Map params) {
    addMapBasedChange(AddLookupTableChange, params, ['existingTableName', 'existingTableSchemaName', 'existingColumnName', 'newTableName', 'newTableSchemaName', 'newColumnName', 'newColumnDataType', 'constraintName'])
  }
  
  void addNotNullConstraint(Map params) {
    addMapBasedChange(AddNotNullConstraintChange, params, ['schemaName', 'tableName', 'columnName', 'defaultNullValue', 'columnDataType'])
  }
  
  void dropNotNullConstraint(Map params) {
    
  }
  
  void addUniqueConstraint(Map params) {
    
  }
  
  void dropUniqueConstraint(Map params) {
    
  }
  
  void createSequence(Map params) {
    
  }
  
  void dropSequence(Map params) {
    
  }
  
  void addAutoIncrement(Map params) {
    
  }
  
  void addDefaultValue(Map params) {
    
  }
  
  void dropDefaultValue(Map params) {
    
  }
  
  void addForeignKeyConstraint(Map params) {
    
  }
  
  void dropForeignKeyConstraint(Map params) {
    
  }
  
  void addPrimaryKey(Map params) {
    
  }
  
  void dropPrimaryKey(Map params) {
    
  }
  
  void insert(Map params, Closure closure) {
    
  }
  
  void loadData(Map params, Closure clousre) {
    
  }
  
  void loadUpdateData(Map params, Closure closure) {
    
  }
  
  void update(Map params, Closure closure) {
    
  }
  
  void delete(Map params, Closure closure) {
    
  }
  
  void tagDatabase(Map params) {
    
  }
  
  void stop(Map params) {
    
  }
  
  void createIndex(Map params, Closure closure) {
    
  }
  
  void dropIndex(Map params) {
    
  }
  
  void sql(Map params, Closure closure) {
    
  }
  
  void sqlFile(Map params) {
    
  }
  
  void customChange(Map params, Closure closure) {
    
  }

  /**
   * A Groovy-specific extension that allows a closure to be provided,
   * implementing the change. The closure is passed the instance of
   * Database.
   */
  void customChange(Closure closure) {
    
  }
  
  void executeCommand(Map params, Closure closure) {
    
  }


  def makeColumnarChangeFromMap(Class klass, Closure closure, Map params, List paramNames) {
    def change = makeChangeFromMap(klass, params, paramNames)

    def columnDelegate = new ColumnDelegate()
    closure.delegate = columnDelegate
    closure.call()

    columnDelegate.columns.each { column ->
      change.addColumn(column)
    }

    return change
  }

  
  private def makeChangeFromMap(Class klass, Map sourceMap, List paramNames) {
    def change = klass.newInstance()
    paramNames.each { name ->
      change[name] = sourceMap[name]
    }

    return change
  }


  private void addMapBasedChange(Class klass, Map sourceMap, List paramNames) {
    changeSet.addChange(makeChangeFromMap(klass, sourceMap, paramNames))
  }


}