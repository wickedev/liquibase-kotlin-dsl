import org.liquibase.kotlin.*

databaseChangeLog(logicalFilePath = ".") {
	preConditions {
		sqlCheck(expectedResult = 1) {
			""
		}
	}
	changeSet(id = "1", author = "Jason Blackwell") {
	}
}