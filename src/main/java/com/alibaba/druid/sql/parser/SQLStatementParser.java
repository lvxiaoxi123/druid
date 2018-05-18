/*
 * Copyright 1999-2017 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.druid.sql.parser;

import com.alibaba.druid.sql.ast.*;
import com.alibaba.druid.sql.ast.expr.SQLCharExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLQueryExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.ast.statement.SQLCreateTriggerStatement.TriggerEvent;
import com.alibaba.druid.sql.ast.statement.SQLCreateTriggerStatement.TriggerType;
import com.alibaba.druid.sql.dialect.oracle.parser.OracleExprParser;
import com.alibaba.druid.util.JdbcConstants;

import java.util.ArrayList;
import java.util.List;

public class SQLStatementParser extends SQLParser {

    protected SQLExprParser exprParser;

    protected boolean       parseCompleteValues = true;

    protected int           parseValuesSize     = 3;

    public SQLStatementParser(String sql){
        this(sql, null);
    }

    public SQLStatementParser(String sql, String dbType){
        this(new SQLExprParser(sql, dbType));
    }

    public SQLStatementParser(SQLExprParser exprParser){
        super(exprParser.getLexer(), exprParser.getDbType());
        this.exprParser = exprParser;
    }

    protected SQLStatementParser(Lexer lexer, String dbType){
        super(lexer, dbType);
    }

    public boolean isKeepComments() {
        return lexer.isKeepComments();
    }

    public void setKeepComments(boolean keepComments) {
        this.lexer.setKeepComments(keepComments);
    }

    public SQLExprParser getExprParser() {
        return exprParser;
    }

    public List<SQLStatement> parseStatementList() {
        List<SQLStatement> statementList = new ArrayList<SQLStatement>();
        parseStatementList(statementList, -1, null);
        return statementList;
    }

    public List<SQLStatement> parseStatementList(SQLObject parent) {
        List<SQLStatement> statementList = new ArrayList<SQLStatement>();
        parseStatementList(statementList, -1, parent);
        return statementList;
    }

    public void parseStatementList(List<SQLStatement> statementList) {
        parseStatementList(statementList, -1);
    }

    public void parseStatementList(List<SQLStatement> statementList, int max) {
        parseStatementList(statementList, max, null);
    }

    public void parseStatementList(List<SQLStatement> statementList, int max, SQLObject parent) {
        for (;;) {
            if (max != -1) {
                if (statementList.size() >= max) {
                    return;
                }
            }

            switch (lexer.token()) {
                case EOF:
                case END:
                case UNTIL:
                case ELSE:
                case WHEN:
                    if (lexer.isKeepComments() && lexer.hasComment() && statementList.size() > 0) {
                        SQLStatement stmt = statementList.get(statementList.size() - 1);
                        stmt.addAfterComment(lexer.readAndResetComments());
                    }
                    return;
                case SEMI: {
                    int line0 = lexer.getLine();
                    lexer.nextToken();
                    int line1 = lexer.getLine();

                    if(statementList.size() > 0) {
                        SQLStatement lastStmt = statementList.get(statementList.size() - 1);
                        lastStmt.setAfterSemi(true);

                        if (lexer.isKeepComments()) {
                            SQLStatement stmt = statementList.get(statementList.size() - 1);
                            if (line1 - line0 <= 1) {
                                stmt.addAfterComment(lexer.readAndResetComments());
                            }
                        }
                    }

                    continue;
                }
                case WITH:
                if (!JdbcConstants.POSTGRESQL.equals(dbType)){
                    SQLStatement stmt = parseSelect();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                break;
                case SELECT: {
                    SQLStatement stmt = parseSelect();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                case UPDATE: {
                    SQLStatement stmt = parseUpdateStatement();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                case CREATE: {
                    SQLStatement stmt = parseCreate();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                case INSERT: {
                    SQLStatement stmt = parseInsert();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                case DELETE: {
                    SQLStatement stmt = parseDeleteStatement();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                case EXPLAIN: {
                    SQLStatement stmt = parseExplain();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                case SET: {
                    SQLStatement stmt = parseSet();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                case ALTER: {
                    SQLStatement stmt = parseAlter();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                case TRUNCATE: {
                    SQLStatement stmt = parseTruncate();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                case USE: {
                    SQLStatement stmt = parseUse();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                case GRANT: {
                    SQLStatement stmt = parseGrant();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                case REVOKE: {
                    SQLStatement stmt = parseRevoke();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                case SHOW: {
                    SQLStatement stmt = parseShow();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                case MERGE: {
                    SQLStatement stmt = parseMerge();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                case REPEAT: {
                    SQLStatement stmt = parseRepeat();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                case DECLARE: {
                    SQLStatement stmt = parseDeclare();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                case WHILE: {
                    SQLStatement stmt = parseWhile();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                case IF: {
                    SQLStatement stmt = parseIf();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                case CASE: {
                    SQLStatement stmt = parseCase();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                case OPEN: {
                    SQLStatement stmt = parseOpen();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                case FETCH: {
                    SQLStatement stmt = parseFetch();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                case DROP: {
                    SQLStatement stmt = parseDrop();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                case COMMENT: {
                    SQLStatement stmt = parseComment();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                case KILL: {
                    SQLStatement stmt = parseKill();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                case CLOSE: {
                    SQLStatement stmt = parseClose();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                case RETURN: {
                    SQLStatement stmt = parseReturn();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                case UPSERT: {
                    SQLStatement stmt = parseUpsert();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                case LEAVE: {
                    SQLStatement stmt = parseLeave();
                    stmt.setParent(parent);
                    statementList.add(stmt);
                    continue;
                }
                default:
                    break;
            }

            if (lexer.token() == Token.LBRACE || identifierEquals("CALL")) {
                SQLCallStatement stmt = parseCall();
                statementList.add(stmt);
                continue;
            }


            if (identifierEquals("UPSERT")) {
                SQLStatement stmt = parseUpsert();
                statementList.add(stmt);
                continue;
            }

            if (identifierEquals("RENAME")) {
                SQLStatement stmt = parseRename();
                statementList.add(stmt);
                continue;
            }

            if (identifierEquals("RELEASE")) {
                SQLStatement stmt = parseReleaseSavePoint();
                statementList.add(stmt);
                continue;
            }

            if (identifierEquals("SAVEPOINT")) {
                SQLStatement stmt = parseSavePoint();
                statementList.add(stmt);
                continue;
            }

            if (identifierEquals("ROLLBACK")) {
                SQLRollbackStatement stmt = parseRollback();

                statementList.add(stmt);
                continue;
            }

            if (identifierEquals("COMMIT")) {
                SQLStatement stmt = parseCommit();

                statementList.add(stmt);
                continue;
            }

            if (identifierEquals("RETURN")) {
                SQLStatement stmt = parseReturn();
                statementList.add(stmt);
                continue;
            }

            if (lexer.token() == Token.LPAREN) {
                char markChar = lexer.current();
                int markBp = lexer.bp();
                lexer.nextToken();
                if (lexer.token() == Token.SELECT) {
                    lexer.reset(markBp, markChar, Token.LPAREN);
                    SQLStatement stmt = parseSelect();
                    statementList.add(stmt);
                    continue;
                }
            }

            if (parseStatementListDialect(statementList)) {
                continue;
            }

            // throw new ParserException("syntax error, " + lexer.token() + " "
            // + lexer.stringVal() + ", pos "
            // + lexer.pos());
            //throw new ParserException("not supported." + lexer.info());
            printError(lexer.token());
        }
    }


    public SQLStatement parseDrop() {
        List<String> beforeComments = null;
        if (lexer.isKeepComments() && lexer.hasComment()) {
            beforeComments = lexer.readAndResetComments();
        }

        lexer.nextToken();

        final SQLStatement stmt;
        if (lexer.token() == Token.TABLE || identifierEquals("TEMPORARY")) {
            stmt = parseDropTable(false);
        } else if (lexer.token() == Token.USER) {
            stmt = parseDropUser();
        } else if (lexer.token() == Token.INDEX) {
            stmt = parseDropIndex();
        } else if (lexer.token() == Token.VIEW) {
            stmt = parseDropView(false);
        } else if (lexer.token() == Token.TRIGGER) {
            stmt = parseDropTrigger(false);
        } else if (lexer.token() == Token.DATABASE) {
            stmt = parseDropDatabase(false);
        } else if (lexer.token() == Token.FUNCTION) {
            stmt = parseDropFunction(false);
        } else if (lexer.token() == Token.TABLESPACE) {
            stmt = parseDropTablespace(false);

        } else if (lexer.token() == Token.PROCEDURE) {
            stmt = parseDropProcedure(false);

        } else if (lexer.token() == Token.SEQUENCE) {
            stmt =  parseDropSequece(false);

        } else {
            throw new ParserException("TODO " + lexer.token());
        }

        if (beforeComments != null) {
            stmt.addBeforeComment(beforeComments);
        }
        return stmt;
    }

    public SQLStatement parseKill() {
        throw new ParserException("not supported. " + lexer.info());
    }

    public SQLStatement parseCase() {
        throw new ParserException("not supported. " + lexer.info());
    }

    public SQLStatement parseIf() {
        throw new ParserException("not supported. " + lexer.info());
    }

    public SQLStatement parseWhile() {
        throw new ParserException("not supported. " + lexer.info());
    }

    public SQLStatement parseDeclare() {
        throw new ParserException("not supported. " + lexer.info());
    }

    public SQLStatement parseRepeat() {
        throw new ParserException("not supported. " + lexer.info());
    }

    public SQLStatement parseLeave() {
        throw new ParserException("not supported. " + lexer.info());
    }

    public SQLStatement parseReturn() {
        if (lexer.token() == Token.RETURN
                || identifierEquals("RETURN")) {
            lexer.nextToken();
        }

        SQLReturnStatement stmt = new SQLReturnStatement();
        if (lexer.token() != Token.SEMI) {
            SQLExpr expr = this.exprParser.expr();
            stmt.setExpr(expr);
        }

        accept(Token.SEMI);
        stmt.setAfterSemi(true);

        return stmt;
    }

    public SQLStatement parseUpsert() {
        SQLInsertStatement insertStatement = new SQLInsertStatement();

        if (lexer.token() == Token.UPSERT || identifierEquals("UPSERT")) {
            lexer.nextToken();
            insertStatement.setUpsert(true);
        }

        parseInsert0(insertStatement);
        return insertStatement;
    }

    public SQLRollbackStatement parseRollback() {
        lexer.nextToken();

        if (identifierEquals("WORK")) {
            lexer.nextToken();
        }

        SQLRollbackStatement stmt = new SQLRollbackStatement(getDbType());

        if (lexer.token() == Token.TO) {
            lexer.nextToken();

            if (identifierEquals("SAVEPOINT") || lexer.token() == Token.SAVEPOINT) {
                lexer.nextToken();
            }

            stmt.setTo(this.exprParser.name());
        }
        return stmt;
    }

    public SQLStatement parseCommit() {
        throw new ParserException("TODO " + lexer.info());
    }

    public SQLStatement parseShow() {
        throw new ParserException("TODO " + lexer.info());
    }

    public SQLUseStatement parseUse() {
        accept(Token.USE);
        SQLUseStatement stmt = new SQLUseStatement(getDbType());
        stmt.setDatabase(this.exprParser.name());
        return stmt;
    }

    public SQLGrantStatement parseGrant() {
        accept(Token.GRANT);
        SQLGrantStatement stmt = new SQLGrantStatement(getDbType());

        parsePrivileages(stmt.getPrivileges(), stmt);

        if (lexer.token() == Token.ON) {
            lexer.nextToken();

            if (lexer.token() == Token.PROCEDURE) {
                lexer.nextToken();
                stmt.setObjectType(SQLObjectType.PROCEDURE);
            } else if (lexer.token() == Token.FUNCTION) {
                lexer.nextToken();
                stmt.setObjectType(SQLObjectType.FUNCTION);
            } else if (lexer.token() == Token.TABLE) {
                lexer.nextToken();
                stmt.setObjectType(SQLObjectType.TABLE);
            } else if (lexer.token() == Token.USER) {
                lexer.nextToken();
                stmt.setObjectType(SQLObjectType.USER);
            } else if (lexer.token() == Token.DATABASE) {
                lexer.nextToken();
                stmt.setObjectType(SQLObjectType.DATABASE);
            }

            if (stmt.getObjectType() != null && lexer.token() == Token.COLONCOLON) {
                lexer.nextToken(); // sql server
            }

            SQLExpr expr = this.exprParser.expr();
            if (stmt.getObjectType() == SQLObjectType.TABLE || stmt.getObjectType() == null) {
                stmt.setOn(new SQLExprTableSource(expr));
            } else {
                stmt.setOn(expr);
            }
        }

        if (lexer.token() == Token.TO) {
            lexer.nextToken();
            stmt.setTo(this.exprParser.expr());
        }

        if (lexer.token() == Token.WITH) {
            lexer.nextToken();

            for (;;) {
                if (identifierEquals("MAX_QUERIES_PER_HOUR")) {
                    lexer.nextToken();
                    stmt.setMaxQueriesPerHour(this.exprParser.primary());
                    continue;
                }

                if (identifierEquals("MAX_UPDATES_PER_HOUR")) {
                    lexer.nextToken();
                    stmt.setMaxUpdatesPerHour(this.exprParser.primary());
                    continue;
                }

                if (identifierEquals("MAX_CONNECTIONS_PER_HOUR")) {
                    lexer.nextToken();
                    stmt.setMaxConnectionsPerHour(this.exprParser.primary());
                    continue;
                }

                if (identifierEquals("MAX_USER_CONNECTIONS")) {
                    lexer.nextToken();
                    stmt.setMaxUserConnections(this.exprParser.primary());
                    continue;
                }

                break;
            }
        }

        if (identifierEquals("ADMIN")) {
            lexer.nextToken();
            acceptIdentifier("OPTION");
            stmt.setAdminOption(true);
        }

        if (lexer.token() == Token.IDENTIFIED) {
            lexer.nextToken();
            accept(Token.BY);
            stmt.setIdentifiedBy(this.exprParser.expr());
        }

        return stmt;
    }

    protected void parsePrivileages(List<SQLExpr> privileges, SQLObject parent) {
        for (;;) {
            String privilege = null;
            if (lexer.token() == Token.ALL) {
                lexer.nextToken();
                if (identifierEquals("PRIVILEGES")) {
                    privilege = "ALL PRIVILEGES";
                } else {
                    privilege = "ALL";
                }
            } else if (lexer.token() == Token.SELECT) {
                privilege = "SELECT";
                lexer.nextToken();
            } else if (lexer.token() == Token.UPDATE) {
                privilege = "UPDATE";
                lexer.nextToken();
            } else if (lexer.token() == Token.DELETE) {
                privilege = "DELETE";
                lexer.nextToken();
            } else if (lexer.token() == Token.INSERT) {
                privilege = "INSERT";
                lexer.nextToken();
            } else if (lexer.token() == Token.INDEX) {
                lexer.nextToken();
                privilege = "INDEX";
            } else if (lexer.token() == Token.TRIGGER) {
                lexer.nextToken();
                privilege = "TRIGGER";
            } else if (lexer.token() == Token.REFERENCES) {
                privilege = "REFERENCES";
                lexer.nextToken();
            } else if (lexer.token() == Token.CREATE) {
                lexer.nextToken();

                if (lexer.token() == Token.TABLE) {
                    privilege = "CREATE TABLE";
                    lexer.nextToken();
                } else if (lexer.token() == Token.SESSION) {
                    privilege = "CREATE SESSION";
                    lexer.nextToken();
                } else if (lexer.token() == Token.TABLESPACE) {
                    privilege = "CREATE TABLESPACE";
                    lexer.nextToken();
                } else if (lexer.token() == Token.USER) {
                    privilege = "CREATE USER";
                    lexer.nextToken();
                } else if (lexer.token() == Token.VIEW) {
                    privilege = "CREATE VIEW";
                    lexer.nextToken();
                } else if (lexer.token() == Token.ANY) {
                    lexer.nextToken();

                    if (lexer.token() == Token.TABLE) {
                        lexer.nextToken();
                        privilege = "CREATE ANY TABLE";
                    } else if (identifierEquals("MATERIALIZED")) {
                        lexer.nextToken();
                        accept(Token.VIEW);
                        privilege = "CREATE ANY MATERIALIZED VIEW";
                    } else {
                        throw new ParserException("TODO : " + lexer.token() + " " + lexer.stringVal());
                    }
                } else if (identifierEquals("SYNONYM")) {
                    privilege = "CREATE SYNONYM";
                    lexer.nextToken();
                } else if (identifierEquals("ROUTINE")) {
                    privilege = "CREATE ROUTINE";
                    lexer.nextToken();
                } else if (identifierEquals("TEMPORARY")) {
                    lexer.nextToken();
                    accept(Token.TABLE);
                    privilege = "CREATE TEMPORARY TABLE";
                } else {
                    throw new ParserException("TODO : " + lexer.token() + " " + lexer.stringVal());
                }
            } else if (lexer.token() == Token.ALTER) {
                lexer.nextToken();
                if (lexer.token() == Token.TABLE) {
                    privilege = "ALTER TABLE";
                    lexer.nextToken();
                } else if (lexer.token() == Token.SESSION) {
                    privilege = "ALTER SESSION";
                    lexer.nextToken();
                } else if (lexer.token() == Token.ANY) {
                    lexer.nextToken();

                    if (lexer.token() == Token.TABLE) {
                        lexer.nextToken();
                        privilege = "ALTER ANY TABLE";
                    } else if (identifierEquals("MATERIALIZED")) {
                        lexer.nextToken();
                        accept(Token.VIEW);
                        privilege = "ALTER ANY MATERIALIZED VIEW";
                    } else {
                        throw new ParserException("TODO : " + lexer.token() + " " + lexer.stringVal());
                    }
                } else {
                    throw new ParserException("TODO : " + lexer.token() + " " + lexer.stringVal());
                }
            } else if (lexer.token() == Token.DROP) {
                lexer.nextToken();
                if (lexer.token() == Token.DROP) {
                    privilege = "DROP TABLE";
                    lexer.nextToken();
                } else if (lexer.token() == Token.SESSION) {
                    privilege = "DROP SESSION";
                    lexer.nextToken();
                } else if (lexer.token() == Token.ANY) {
                    lexer.nextToken();

                    if (lexer.token() == Token.TABLE) {
                        lexer.nextToken();
                        privilege = "DROP ANY TABLE";
                    } else if (identifierEquals("MATERIALIZED")) {
                        lexer.nextToken();
                        accept(Token.VIEW);
                        privilege = "DROP ANY MATERIALIZED VIEW";
                    } else {
                        throw new ParserException("TODO : " + lexer.token() + " " + lexer.stringVal());
                    }
                } else {
                    privilege = "DROP";
                }
            } else if (identifierEquals("USAGE")) {
                privilege = "USAGE";
                lexer.nextToken();
            } else if (identifierEquals("EXECUTE")) {
                privilege = "EXECUTE";
                lexer.nextToken();
            } else if (identifierEquals("PROXY")) {
                privilege = "PROXY";
                lexer.nextToken();
            } else if (identifierEquals("QUERY")) {
                lexer.nextToken();
                acceptIdentifier("REWRITE");
                privilege = "QUERY REWRITE";
            } else if (identifierEquals("GLOBAL")) {
                lexer.nextToken();
                acceptIdentifier("QUERY");
                acceptIdentifier("REWRITE");
                privilege = "GLOBAL QUERY REWRITE";
            } else if (identifierEquals("INHERIT")) {
                lexer.nextToken();
                acceptIdentifier("PRIVILEGES");
                privilege = "INHERIT PRIVILEGES";
            } else if (identifierEquals("EVENT")) {
                lexer.nextToken();
                privilege = "EVENT";
            } else if (identifierEquals("FILE")) {
                lexer.nextToken();
                privilege = "FILE";
            } else if (lexer.token() == Token.GRANT) {
                lexer.nextToken();
                acceptIdentifier("OPTION");
                privilege = "GRANT OPTION";
            } else if (lexer.token() == Token.LOCK) {
                lexer.nextToken();
                acceptIdentifier("TABLES");
                privilege = "LOCK TABLES";
            } else if (identifierEquals("PROCESS")) {
                lexer.nextToken();
                privilege = "PROCESS";
            } else if (identifierEquals("RELOAD")) {
                lexer.nextToken();
                privilege = "RELOAD";
            } else if (identifierEquals("REPLICATION")) {
                lexer.nextToken();
                if (identifierEquals("SLAVE")) {
                    lexer.nextToken();
                    privilege = "REPLICATION SLAVE";
                } else {
                    acceptIdentifier("CLIENT");
                    privilege = "REPLICATION CLIENT";
                }
            } else if (lexer.token() == Token.SHOW) {
                lexer.nextToken();

                if (lexer.token() == Token.VIEW) {
                    lexer.nextToken();
                    privilege = "SHOW VIEW";
                } else {
                    acceptIdentifier("DATABASES");
                    privilege = "SHOW DATABASES";
                }
            } else if (identifierEquals("SHUTDOWN")) {
                lexer.nextToken();
                privilege = "SHUTDOWN";
            } else if (identifierEquals("SUPER")) {
                lexer.nextToken();
                privilege = "SUPER";

            } else if (identifierEquals("CONTROL")) { // sqlserver
                lexer.nextToken();
                privilege = "CONTROL";
            } else if (identifierEquals("IMPERSONATE")) { // sqlserver
                lexer.nextToken();
                privilege = "IMPERSONATE";
            }

            if (privilege != null) {
                SQLExpr expr = new SQLIdentifierExpr(privilege);

                if (lexer.token() == Token.LPAREN) {
                    expr = this.exprParser.primaryRest(expr);
                }

                expr.setParent(parent);
                privileges.add(expr);
            }

            if (lexer.token() == Token.COMMA) {
                lexer.nextToken();
                continue;
            }
            break;
        }
    }

    public SQLRevokeStatement parseRevoke() {
        accept(Token.REVOKE);

        SQLRevokeStatement stmt = new SQLRevokeStatement(getDbType());

        parsePrivileages(stmt.getPrivileges(), stmt);

        if (lexer.token() == Token.ON) {
            lexer.nextToken();

            if (lexer.token() == Token.PROCEDURE) {
                lexer.nextToken();
                stmt.setObjectType(SQLObjectType.PROCEDURE);
            } else if (lexer.token() == Token.FUNCTION) {
                lexer.nextToken();
                stmt.setObjectType(SQLObjectType.FUNCTION);
            } else if (lexer.token() == Token.TABLE) {
                lexer.nextToken();
                stmt.setObjectType(SQLObjectType.TABLE);
            } else if (lexer.token() == Token.USER) {
                lexer.nextToken();
                stmt.setObjectType(SQLObjectType.USER);
            }

            SQLExpr expr = this.exprParser.expr();
            if (stmt.getObjectType() == SQLObjectType.TABLE || stmt.getObjectType() == null) {
                stmt.setOn(new SQLExprTableSource(expr));
            } else {
                stmt.setOn(expr);
            }
        }

        if (lexer.token() == Token.FROM) {
            lexer.nextToken();
            stmt.setFrom(this.exprParser.expr());
        }

        return stmt;
    }

    public SQLStatement parseSavePoint() {
        acceptIdentifier("SAVEPOINT");
        SQLSavePointStatement stmt = new SQLSavePointStatement(getDbType());
        stmt.setName(this.exprParser.name());
        return stmt;
    }

    public SQLStatement parseReleaseSavePoint() {
        acceptIdentifier("RELEASE");
        acceptIdentifier("SAVEPOINT");
        SQLReleaseSavePointStatement stmt = new SQLReleaseSavePointStatement(getDbType());
        stmt.setName(this.exprParser.name());
        return stmt;
    }

    public SQLStatement parseAlter() {
        accept(Token.ALTER);

        if (lexer.token() == Token.TABLE) {
            lexer.nextToken();

            SQLAlterTableStatement stmt = new SQLAlterTableStatement(getDbType());
            stmt.setName(this.exprParser.name());

            for (;;) {
                if (lexer.token() == Token.DROP) {
                    parseAlterDrop(stmt);
                } else if (identifierEquals("ADD")) {
                    lexer.nextToken();

                    boolean ifNotExists = false;

                    if (lexer.token() == Token.IF) {
                        lexer.nextToken();
                        accept(Token.NOT);
                        accept(Token.EXISTS);
                        ifNotExists = true;
                    }

                    if (lexer.token() == Token.PRIMARY) {
                        SQLPrimaryKey primaryKey = this.exprParser.parsePrimaryKey();
                        SQLAlterTableAddConstraint item = new SQLAlterTableAddConstraint(primaryKey);
                        stmt.addItem(item);
                    } else if (lexer.token() == Token.IDENTIFIER) {
                        SQLAlterTableAddColumn item = parseAlterTableAddColumn();
                        stmt.addItem(item);
                    } else if (lexer.token() == Token.COLUMN) {
                        lexer.nextToken();
                        SQLAlterTableAddColumn item = parseAlterTableAddColumn();
                        stmt.addItem(item);
                    } else if (lexer.token() == Token.CHECK) {
                        SQLCheck check = this.exprParser.parseCheck();
                        SQLAlterTableAddConstraint item = new SQLAlterTableAddConstraint(check);
                        stmt.addItem(item);
                    } else if (lexer.token() == Token.CONSTRAINT) {
                        SQLConstraint constraint = this.exprParser.parseConstaint();
                        SQLAlterTableAddConstraint item = new SQLAlterTableAddConstraint(constraint);
                        stmt.addItem(item);
                    } else if (lexer.token() == Token.FOREIGN) {
                        SQLConstraint constraint = this.exprParser.parseForeignKey();
                        SQLAlterTableAddConstraint item = new SQLAlterTableAddConstraint(constraint);
                        stmt.addItem(item);
                    } else if (lexer.token() == Token.PARTITION) {
                        lexer.nextToken();
                        SQLAlterTableAddPartition addPartition = new SQLAlterTableAddPartition();

                        addPartition.setIfNotExists(ifNotExists);

                        accept(Token.LPAREN);

                        parseAssignItems(addPartition.getPartitions(), addPartition);

                        accept(Token.RPAREN);

                        stmt.addItem(addPartition);
                    } else {
                        throw new ParserException("TODO " + lexer.info());
                    }
                } else if (lexer.token() == Token.DISABLE) {
                    lexer.nextToken();

                    if (lexer.token() == Token.CONSTRAINT) {
                        lexer.nextToken();
                        SQLAlterTableDisableConstraint item = new SQLAlterTableDisableConstraint();
                        item.setConstraintName(this.exprParser.name());
                        stmt.addItem(item);
                    } else if (identifierEquals("LIFECYCLE")) {
                        lexer.nextToken();
                        SQLAlterTableDisableLifecycle item = new SQLAlterTableDisableLifecycle();
                        stmt.addItem(item);
                    } else {
                        acceptIdentifier("KEYS");
                        SQLAlterTableDisableKeys item = new SQLAlterTableDisableKeys();
                        stmt.addItem(item);
                    }
                } else if (lexer.token() == Token.ENABLE) {
                    lexer.nextToken();
                    if (lexer.token() == Token.CONSTRAINT) {
                        lexer.nextToken();
                        SQLAlterTableEnableConstraint item = new SQLAlterTableEnableConstraint();
                        item.setConstraintName(this.exprParser.name());
                        stmt.addItem(item);
                    } else if (identifierEquals("LIFECYCLE")) {
                        lexer.nextToken();
                        SQLAlterTableEnableLifecycle item = new SQLAlterTableEnableLifecycle();
                        stmt.addItem(item);
                    } else {
                        acceptIdentifier("KEYS");
                        SQLAlterTableEnableKeys item = new SQLAlterTableEnableKeys();
                        stmt.addItem(item);
                    }
                } else if (lexer.token() == Token.ALTER) {
                    lexer.nextToken();
                    if (lexer.token() == Token.COLUMN) {
                        SQLAlterTableAlterColumn alterColumn = parseAlterColumn();
                        stmt.addItem(alterColumn);
                    } else if (lexer.token() == Token.LITERAL_ALIAS) {
                        SQLAlterTableAlterColumn alterColumn = parseAlterColumn();
                        stmt.addItem(alterColumn);
                    } else {
                        throw new ParserException("TODO " + lexer.info());
                    }
                } else if (identifierEquals("CHANGE")) {
                    lexer.nextToken();
                    accept(Token.COLUMN);
                    SQLName columnName = this.exprParser.name();

                    if (identifierEquals("RENAME")) {
                        lexer.nextToken();
                        accept(Token.TO);
                        SQLName toName = this.exprParser.name();
                        SQLAlterTableRenameColumn renameColumn = new SQLAlterTableRenameColumn();

                        renameColumn.setColumn(columnName);
                        renameColumn.setTo(toName);

                        stmt.addItem(renameColumn);
                    } else if (lexer.token() == Token.COMMENT) {
                        lexer.nextToken();
                        SQLExpr comment = this.exprParser.primary();

                        SQLColumnDefinition column = new SQLColumnDefinition();
                        column.setDbType(dbType);
                        column.setName(columnName);
                        column.setComment(comment);

                        SQLAlterTableAlterColumn changeColumn = new SQLAlterTableAlterColumn();

                        changeColumn.setColumn(column);

                        stmt.addItem(changeColumn);
                    } else {
                        SQLColumnDefinition column = this.exprParser.parseColumn();

                        SQLAlterTableAlterColumn alterColumn = new SQLAlterTableAlterColumn();
                        alterColumn.setColumn(column);
                        alterColumn.setOriginColumn(columnName);
                        stmt.addItem(alterColumn);
                    }
                } else if (lexer.token() == Token.WITH) {
                    lexer.nextToken();
                    acceptIdentifier("NOCHECK");
                    acceptIdentifier("ADD");
                    SQLConstraint check = this.exprParser.parseConstaint();

                    SQLAlterTableAddConstraint addCheck = new SQLAlterTableAddConstraint();
                    addCheck.setWithNoCheck(true);
                    addCheck.setConstraint(check);
                    stmt.addItem(addCheck);
                } else if (identifierEquals("RENAME")) {
                    stmt.addItem(parseAlterTableRename());
                } else if (lexer.token() == Token.SET) {
                    lexer.nextToken();

                    if (lexer.token() == Token.COMMENT) {
                        lexer.nextToken();
                        SQLAlterTableSetComment setComment = new SQLAlterTableSetComment();
                        setComment.setComment(this.exprParser.primary());
                        stmt.addItem(setComment);
                    } else if (identifierEquals("LIFECYCLE")) {
                        lexer.nextToken();
                        SQLAlterTableSetLifecycle setLifecycle = new SQLAlterTableSetLifecycle();
                        setLifecycle.setLifecycle(this.exprParser.primary());
                        stmt.addItem(setLifecycle);
                    } else {
                        throw new ParserException("TODO " + lexer.info());
                    }
                } else if (lexer.token() == Token.PARTITION) {
                    lexer.nextToken();

                    SQLAlterTableRenamePartition renamePartition = new SQLAlterTableRenamePartition();

                    accept(Token.LPAREN);

                    parseAssignItems(renamePartition.getPartition(), renamePartition);

                    accept(Token.RPAREN);

                    if (lexer.token() == Token.ENABLE) {
                        lexer.nextToken();
                        if (identifierEquals("LIFECYCLE")) {
                            lexer.nextToken();
                        }

                        SQLAlterTableEnableLifecycle enableLifeCycle = new SQLAlterTableEnableLifecycle();
                        for (SQLAssignItem condition : renamePartition.getPartition()) {
                            enableLifeCycle.getPartition().add(condition);
                            condition.setParent(enableLifeCycle);
                        }
                        stmt.addItem(enableLifeCycle);

                        continue;
                    }

                    if (lexer.token() == Token.DISABLE) {
                        lexer.nextToken();
                        if (identifierEquals("LIFECYCLE")) {
                            lexer.nextToken();
                        }

                        SQLAlterTableDisableLifecycle disableLifeCycle = new SQLAlterTableDisableLifecycle();
                        for (SQLAssignItem condition : renamePartition.getPartition()) {
                            disableLifeCycle.getPartition().add(condition);
                            condition.setParent(disableLifeCycle);
                        }
                        stmt.addItem(disableLifeCycle);

                        continue;
                    }

                    acceptIdentifier("RENAME");
                    accept(Token.TO);
                    accept(Token.PARTITION);

                    accept(Token.LPAREN);

                    parseAssignItems(renamePartition.getTo(), renamePartition);

                    accept(Token.RPAREN);

                    stmt.addItem(renamePartition);
                } else if (identifierEquals("TOUCH")) {
                    lexer.nextToken();
                    SQLAlterTableTouch item = new SQLAlterTableTouch();

                    if (lexer.token() == Token.PARTITION) {
                        lexer.nextToken();

                        accept(Token.LPAREN);
                        parseAssignItems(item.getPartition(), item);
                        accept(Token.RPAREN);
                    }

                    stmt.addItem(item);
                } else if (JdbcConstants.ODPS.equals(dbType) && identifierEquals("MERGE")) {
                    lexer.nextToken();
                    acceptIdentifier("SMALLFILES");
                    stmt.setMergeSmallFiles(true);
                } else {
                    break;
                }
            }

            return stmt;
        } else if (lexer.token() == Token.VIEW) {
            lexer.nextToken();
            SQLName viewName = this.exprParser.name();

            if (identifierEquals("RENAME")) {
                lexer.nextToken();
                accept(Token.TO);

                SQLAlterViewRenameStatement stmt = new SQLAlterViewRenameStatement();
                stmt.setName(viewName);

                SQLName newName = this.exprParser.name();

                stmt.setTo(newName);

                return stmt;
            }
            throw new ParserException("TODO " + lexer.info());
        }
        throw new ParserException("TODO " + lexer.info());
    }

    protected SQLAlterTableItem parseAlterTableRename() {
        acceptIdentifier("RENAME");

        if (lexer.token() == Token.COLUMN) {
            lexer.nextToken();
            SQLAlterTableRenameColumn renameColumn = new SQLAlterTableRenameColumn();
            renameColumn.setColumn(this.exprParser.name());
            accept(Token.TO);
            renameColumn.setTo(this.exprParser.name());
            return renameColumn;
        }

        if (lexer.token() == Token.TO) {
            lexer.nextToken();
            SQLAlterTableRename item = new SQLAlterTableRename();
            item.setTo(this.exprParser.name());
            return item;
        }

        throw new ParserException("TODO " + lexer.info());
    }

    protected SQLAlterTableAlterColumn parseAlterColumn() {
        lexer.nextToken();
        SQLColumnDefinition column = this.exprParser.parseColumn();

        SQLAlterTableAlterColumn alterColumn = new SQLAlterTableAlterColumn();
        alterColumn.setColumn(column);
        return alterColumn;
    }

    public void parseAlterDrop(SQLAlterTableStatement stmt) {
        lexer.nextToken();

        boolean ifExists = false;

        if (lexer.token() == Token.IF) {
            lexer.nextToken();

            accept(Token.EXISTS);
            ifExists = true;
        }

        if (lexer.token() == Token.CONSTRAINT) {
            lexer.nextToken();
            SQLAlterTableDropConstraint item = new SQLAlterTableDropConstraint();
            item.setConstraintName(this.exprParser.name());
            stmt.addItem(item);
        } else if (lexer.token() == Token.COLUMN) {
            lexer.nextToken();
            SQLAlterTableDropColumnItem item = new SQLAlterTableDropColumnItem();
            this.exprParser.names(item.getColumns());

            if (lexer.token == Token.CASCADE) {
                item.setCascade(true);
                lexer.nextToken();
            }

            stmt.addItem(item);
        } else if (lexer.token() == Token.LITERAL_ALIAS) {
            SQLAlterTableDropColumnItem item = new SQLAlterTableDropColumnItem();
            this.exprParser.names(item.getColumns());

            if (lexer.token == Token.CASCADE) {
                item.setCascade(true);
                lexer.nextToken();
            }

            stmt.addItem(item);
        } else if (lexer.token() == Token.PARTITION) {
            SQLAlterTableDropPartition dropPartition = parseAlterTableDropPartition(ifExists);

            stmt.addItem(dropPartition);
        } else if (lexer.token() == Token.INDEX) {
            lexer.nextToken();
            SQLName indexName = this.exprParser.name();
            SQLAlterTableDropIndex item = new SQLAlterTableDropIndex();
            item.setIndexName(indexName);
            stmt.addItem(item);
        } else {
            throw new ParserException("TODO " + lexer.info());
        }
    }

    protected SQLAlterTableDropPartition parseAlterTableDropPartition(boolean ifExists) {
        lexer.nextToken();
        SQLAlterTableDropPartition dropPartition = new SQLAlterTableDropPartition();

        dropPartition.setIfExists(ifExists);

        if (lexer.token() == Token.LPAREN) {
            accept(Token.LPAREN);

            parseAssignItems(dropPartition.getPartitions(), dropPartition);

            accept(Token.RPAREN);
            
            if (identifierEquals("PURGE")) {
                lexer.nextToken();
                dropPartition.setPurge(true);
            }
        } else {
            SQLName partition = this.exprParser.name();
            dropPartition.addPartition(partition);
        }

    
        return dropPartition;
    }

    public SQLStatement parseRename() {
        throw new ParserException("TODO " + lexer.info());
    }

    protected SQLDropTableStatement parseDropTable(boolean acceptDrop) {
        if (acceptDrop) {
            accept(Token.DROP);
        }

        SQLDropTableStatement stmt = new SQLDropTableStatement(getDbType());

        if (identifierEquals("TEMPORARY")) {
            lexer.nextToken();
            stmt.setTemporary(true);
        }

        accept(Token.TABLE);

        if (lexer.token() == Token.IF) {
            lexer.nextToken();
            accept(Token.EXISTS);
            stmt.setIfExists(true);
        }

        for (;;) {
            SQLName name = this.exprParser.name();
            stmt.addPartition(new SQLExprTableSource(name));
            if (lexer.token() == Token.COMMA) {
                lexer.nextToken();
                continue;
            }
            break;
        }

        for (;;) {
            if (identifierEquals("RESTRICT")) {
                lexer.nextToken();
                stmt.setRestrict(true);
                continue;
            }

            if (identifierEquals("CASCADE")) {
                lexer.nextToken();
                stmt.setCascade(true);

                if (identifierEquals("CONSTRAINTS")) { // for oracle
                    lexer.nextToken();
                }

                continue;
            }

            if (lexer.token() == Token.PURGE || identifierEquals("PURGE")) {
                lexer.nextToken();
                stmt.setPurge(true);
                continue;
            }

            break;
        }

        return stmt;
    }

    protected SQLDropSequenceStatement parseDropSequece(boolean acceptDrop) {
        if (acceptDrop) {
            accept(Token.DROP);
        }

        lexer.nextToken();

        SQLName name = this.exprParser.name();

        SQLDropSequenceStatement stmt = new SQLDropSequenceStatement(getDbType());
        stmt.setName(name);
        return stmt;
    }

    protected SQLDropTriggerStatement parseDropTrigger(boolean acceptDrop) {
        if (acceptDrop) {
            accept(Token.DROP);
        }

        lexer.nextToken();

        SQLName name = this.exprParser.name();

        SQLDropTriggerStatement stmt = new SQLDropTriggerStatement(getDbType());
        stmt.setName(name);
        return stmt;
    }

    protected SQLDropViewStatement parseDropView(boolean acceptDrop) {
        if (acceptDrop) {
            accept(Token.DROP);
        }

        SQLDropViewStatement stmt = new SQLDropViewStatement(getDbType());

        accept(Token.VIEW);

        if (lexer.token() == Token.IF) {
            lexer.nextToken();
            accept(Token.EXISTS);
            stmt.setIfExists(true);
        }

        for (;;) {
            SQLName name = this.exprParser.name();
            stmt.addPartition(new SQLExprTableSource(name));
            if (lexer.token() == Token.COMMA) {
                lexer.nextToken();
                continue;
            }
            break;
        }

        if (identifierEquals("RESTRICT")) {
            lexer.nextToken();
            stmt.setRestrict(true);
        } else if (identifierEquals("CASCADE")) {
            lexer.nextToken();

            if (identifierEquals("CONSTRAINTS")) { // for oracle
                lexer.nextToken();
            }

            stmt.setCascade(true);
        }

        return stmt;
    }

    protected SQLDropDatabaseStatement parseDropDatabase(boolean acceptDrop) {
        if (acceptDrop) {
            accept(Token.DROP);
        }

        SQLDropDatabaseStatement stmt = new SQLDropDatabaseStatement(getDbType());

        accept(Token.DATABASE);

        if (lexer.token() == Token.IF) {
            lexer.nextToken();
            accept(Token.EXISTS);
            stmt.setIfExists(true);
        }

        SQLName name = this.exprParser.name();
        stmt.setDatabase(name);

        return stmt;
    }

    protected SQLDropFunctionStatement parseDropFunction(boolean acceptDrop) {
        if (acceptDrop) {
            accept(Token.DROP);
        }

        SQLDropFunctionStatement stmt = new SQLDropFunctionStatement(getDbType());

        accept(Token.FUNCTION);

        if (lexer.token() == Token.IF) {
            lexer.nextToken();
            accept(Token.EXISTS);
            stmt.setIfExists(true);
        }

        SQLName name = this.exprParser.name();
        stmt.setName(name);

        return stmt;
    }

    protected SQLDropTableSpaceStatement parseDropTablespace(boolean acceptDrop) {
        SQLDropTableSpaceStatement stmt = new SQLDropTableSpaceStatement(getDbType());

        if (lexer.isKeepComments() && lexer.hasComment()) {
            stmt.addBeforeComment(lexer.readAndResetComments());
        }

        if (acceptDrop) {
            accept(Token.DROP);
        }

        accept(Token.TABLESPACE);

        if (lexer.token() == Token.IF) {
            lexer.nextToken();
            accept(Token.EXISTS);
            stmt.setIfExists(true);
        }

        SQLName name = this.exprParser.name();
        stmt.setName(name);

        return stmt;
    }

    protected SQLDropProcedureStatement parseDropProcedure(boolean acceptDrop) {
        if (acceptDrop) {
            accept(Token.DROP);
        }

        SQLDropProcedureStatement stmt = new SQLDropProcedureStatement(getDbType());

        accept(Token.PROCEDURE);

        if (lexer.token() == Token.IF) {
            lexer.nextToken();
            accept(Token.EXISTS);
            stmt.setIfExists(true);
        }

        SQLName name = this.exprParser.name();
        stmt.setName(name);

        return stmt;
    }

    public SQLStatement parseTruncate() {
        accept(Token.TRUNCATE);
        if (lexer.token() == Token.TABLE) {
            lexer.nextToken();
        }
        SQLTruncateStatement stmt = new SQLTruncateStatement(getDbType());

        if (lexer.token() == Token.ONLY) {
            lexer.nextToken();
            stmt.setOnly(true);
        }

        for (;;) {
            SQLName name = this.exprParser.name();
            stmt.addTableSource(name);

            if (lexer.token() == Token.COMMA) {
                lexer.nextToken();
                continue;
            }

            break;
        }

        for (;;) {
            if (lexer.token() == Token.PURGE) {
                lexer.nextToken();

                if (identifierEquals("SNAPSHOT")) {
                    lexer.nextToken();
                    acceptIdentifier("LOG");
                    stmt.setPurgeSnapshotLog(true);
                } else {
                    throw new ParserException("TODO : " + lexer.token() + " " + lexer.stringVal());
                }
                continue;
            }

            if (lexer.token() == Token.RESTART) {
                lexer.nextToken();
                accept(Token.IDENTITY);
                stmt.setRestartIdentity(Boolean.TRUE);
                continue;
            } else if (lexer.token() == Token.SHARE) {
                lexer.nextToken();
                accept(Token.IDENTITY);
                stmt.setRestartIdentity(Boolean.FALSE);
                continue;
            }

            if (lexer.token() == Token.CASCADE) {
                lexer.nextToken();
                stmt.setCascade(Boolean.TRUE);
                continue;
            } else if (lexer.token() == Token.RESTRICT) {
                lexer.nextToken();
                stmt.setCascade(Boolean.FALSE);
                continue;
            }
            
            if (lexer.token() == Token.DROP) {
                lexer.nextToken();
                acceptIdentifier("STORAGE");
                stmt.setDropStorage(true);
                continue;
            }
            
            if (identifierEquals("REUSE")) {
                lexer.nextToken();
                acceptIdentifier("STORAGE");
                stmt.setReuseStorage(true);
                continue;
            }
            
            if (identifierEquals("IGNORE")) {
                lexer.nextToken();
                accept(Token.DELETE);
                acceptIdentifier("TRIGGERS");
                stmt.setIgnoreDeleteTriggers(true);
                continue;
            }
            
            if (identifierEquals("RESTRICT")) {
                lexer.nextToken();
                accept(Token.WHEN);
                accept(Token.DELETE);
                acceptIdentifier("TRIGGERS");
                stmt.setRestrictWhenDeleteTriggers(true);
                continue;
            }
            
            if (lexer.token() == Token.CONTINUE) {
                lexer.nextToken();
                accept(Token.IDENTITY);
                continue;
            }
            
            if (identifierEquals("IMMEDIATE")) {
                lexer.nextToken();
                stmt.setImmediate(true);
                continue;
            }

            break;
        }

        return stmt;
    }

    public SQLStatement parseInsert() {
        SQLInsertStatement stmt = new SQLInsertStatement();

        if (lexer.token() == Token.INSERT) {
            accept(Token.INSERT);
        }

        parseInsert0(stmt);
        return stmt;
    }

    protected void parseInsert0(SQLInsertInto insertStatement) {
        parseInsert0(insertStatement, true);
    }

    protected void parseInsert0_hinits(SQLInsertInto insertStatement) {

    }

    protected void parseInsert0(SQLInsertInto insertStatement, boolean acceptSubQuery) {
        if (lexer.token() == Token.INTO) {
            lexer.nextToken();

            SQLName tableName = this.exprParser.name();
            insertStatement.setTableName(tableName);

            if (lexer.token() == Token.LITERAL_ALIAS) {
                insertStatement.setAlias(tableAlias());
            }

            parseInsert0_hinits(insertStatement);

            if (lexer.token() == Token.IDENTIFIER) {
                insertStatement.setAlias(lexer.stringVal());
                lexer.nextToken();
            }
        }

        if (lexer.token() == (Token.LPAREN)) {
            lexer.nextToken();
            parseInsertColumns(insertStatement);
            accept(Token.RPAREN);
        }

        if (lexer.token() == Token.VALUES) {
            lexer.nextToken();
            for (;;) {
                accept(Token.LPAREN);
                SQLInsertStatement.ValuesClause values = new SQLInsertStatement.ValuesClause();
                this.exprParser.exprList(values.getValues(), values);
                insertStatement.addValueCause(values);
                accept(Token.RPAREN);
                
                if (lexer.token() == Token.COMMA) {
                    lexer.nextToken();
                    continue;
                } else {
                    break;
                }
            }
        } else if (acceptSubQuery && (lexer.token() == Token.SELECT || lexer.token() == Token.LPAREN)) {
            SQLSelect select = this.createSQLSelectParser().select();
            insertStatement.setQuery(select);
        }
    }

    protected void parseInsertColumns(SQLInsertInto insert) {
        this.exprParser.exprList(insert.getColumns(), insert);
    }

    public boolean parseStatementListDialect(List<SQLStatement> statementList) {
        return false;
    }

    public SQLDropUserStatement parseDropUser() {
        accept(Token.USER);

        SQLDropUserStatement stmt = new SQLDropUserStatement(getDbType());
        for (;;) {
            SQLExpr expr = this.exprParser.expr();
            stmt.addUser(expr);
            if (lexer.token() == Token.COMMA) {
                lexer.nextToken();
                continue;
            }
            break;
        }

        return stmt;
    }

    public SQLStatement parseDropIndex() {
        accept(Token.INDEX);
        SQLDropIndexStatement stmt = new SQLDropIndexStatement(getDbType());
        stmt.setIndexName(this.exprParser.name());

        if (lexer.token() == Token.ON) {
            lexer.nextToken();
            stmt.setTableName(this.exprParser.name());
        }
        return stmt;
    }

    public SQLCallStatement parseCall() {

        boolean brace = false;
        if (lexer.token() == Token.LBRACE) {
            lexer.nextToken();
            brace = true;
        }

        SQLCallStatement stmt = new SQLCallStatement(getDbType());

        if (lexer.token() == Token.QUES) {
            lexer.nextToken();
            accept(Token.EQ);
            stmt.setOutParameter(new SQLVariantRefExpr("?"));
        }

        acceptIdentifier("CALL");

        stmt.setProcedureName(exprParser.name());

        if (lexer.token() == Token.LPAREN) {
            lexer.nextToken();
            exprParser.exprList(stmt.getParameters(), stmt);
            accept(Token.RPAREN);
        }

        if (brace) {
            accept(Token.RBRACE);
            stmt.setBrace(true);
        }

        return stmt;
    }

    public SQLStatement parseSet() {
        accept(Token.SET);
        SQLSetStatement stmt = new SQLSetStatement(getDbType());

        parseAssignItems(stmt.getItems(), stmt);

        return stmt;
    }

    public void parseAssignItems(List<? super SQLAssignItem> items, SQLObject parent) {
        for (;;) {
            SQLAssignItem item = exprParser.parseAssignItem();
            item.setParent(parent);
            items.add(item);

            if (lexer.token() == Token.COMMA) {
                lexer.nextToken();
                continue;
            } else {
                break;
            }
        }
    }

    public SQLStatement parseCreatePackage() {
        throw new ParserException("TODO " + lexer.info());
    }

    public SQLStatement parseCreate() {
        char markChar = lexer.current();
        int markBp = lexer.bp();
        
        List<String> comments = null;
        if (lexer.isKeepComments() && lexer.hasComment()) {
            comments = lexer.readAndResetComments();
        }

        accept(Token.CREATE);

        Token token = lexer.token();

        if (token == Token.TABLE || identifierEquals("GLOBAL")) {
            SQLCreateTableParser createTableParser = getSQLCreateTableParser();
            SQLCreateTableStatement stmt = createTableParser.parseCrateTable(false);
            
            if (comments != null) {
                stmt.addBeforeComment(comments);
            }
            
            return stmt;
        } else if (token == Token.INDEX //
                   || token == Token.UNIQUE //
                   || identifierEquals("NONCLUSTERED") // sql server
        ) {
            return parseCreateIndex(false);
        } else if (lexer.token() == Token.SEQUENCE) {
            return parseCreateSequence(false);
        } else if (token == Token.OR) {
            lexer.nextToken();
            accept(Token.REPLACE);

            if (identifierEquals("FORCE")) {
                lexer.nextToken();
            }
            if (lexer.token() == Token.PROCEDURE) {
                lexer.reset(markBp, markChar, Token.CREATE);
                return parseCreateProcedure();
            }

            if (lexer.token() == Token.VIEW) {
                lexer.reset(markBp, markChar, Token.CREATE);
                return parseCreateView();
            }

            if (lexer.token() == Token.TRIGGER) {
                lexer.reset(markBp, markChar, Token.CREATE);
                return parseCreateTrigger();
            }

            if (identifierEquals("PACKAGE")) {
                lexer.reset(markBp, markChar, Token.CREATE);
                return parseCreatePackage();
            }

            // lexer.reset(mark_bp, mark_ch, Token.CREATE);
            throw new ParserException("TODO " + lexer.info());
        } else if (token == Token.DATABASE) {
            lexer.nextToken();
            if (identifierEquals("LINK")) {
                lexer.reset(markBp, markChar, Token.CREATE);
                return parseCreateDbLink();
            }

            lexer.reset(markBp, markChar, Token.CREATE);
            return parseCreateDatabase();
        } else if (identifierEquals("PUBLIC") || identifierEquals("SHARE")) {
            lexer.reset(markBp, markChar, Token.CREATE);
            return parseCreateDbLink();
        } else if (token == Token.VIEW) {
            return parseCreateView();
        } else if (token == Token.TRIGGER) {
            lexer.reset(markBp, markChar, Token.CREATE);
            return parseCreateTrigger();
        } else if (token == Token.PROCEDURE) {
            SQLCreateProcedureStatement stmt = parseCreateProcedure();
            stmt.setCreate(true);
            return stmt;
        } else if (token == Token.FUNCTION) {
            lexer.reset(markBp, markChar, Token.CREATE);
            SQLStatement stmt = this.parseCreateFunction();
            return stmt;
        } else if (identifierEquals("BITMAP")) {
            lexer.reset(markBp, markChar, Token.CREATE);
            return parseCreateIndex(true);
        } else if (identifierEquals("MATERIALIZED")) {
            lexer.reset(markBp, markChar, Token.CREATE);
            return parseCreateMaterializedView();
        }

        throw new ParserException("TODO " + lexer.token());
    }

    public SQLCreateFunctionStatement parseCreateFunction() {
        throw new ParserException("TODO " + lexer.token());
    }

    public SQLStatement parseCreateMaterializedView() {
        accept(Token.CREATE);
        acceptIdentifier("MATERIALIZED");
        accept(Token.VIEW);

        SQLCreateMaterializedViewStatement stmt = new SQLCreateMaterializedViewStatement();
        stmt.setName(this.exprParser.name());

        for (;;) {
            if (exprParser instanceof OracleExprParser) {
                ((OracleExprParser) exprParser).parseSegmentAttributes(stmt);
            }

            if (identifierEquals("REFRESH")) {
                lexer.nextToken();

                for (; ; ) {
                    if (identifierEquals("FAST")) {
                        lexer.nextToken();
                        stmt.setRefreshFast(true);
                    } else if (identifierEquals("COMPLETE")) {
                        lexer.nextToken();
                        stmt.setRefreshComlete(true);
                    } else if (identifierEquals("FORCE")) {
                        lexer.nextToken();
                        stmt.setRefreshForce(true);
                    } else if (lexer.token() == Token.ON) {
                        lexer.nextToken();
                        if (lexer.token() == Token.COMMIT) {
                            lexer.nextToken();
                            stmt.setRefreshOnCommit(true);
                        } else {
                            acceptIdentifier("DEMAND");
                            stmt.setRefreshOnDemand(true);
                        }
                    } else {
                        break;
                    }
                }
            } else if (identifierEquals("BUILD")) {
                lexer.nextToken();

                if (identifierEquals("IMMEDIATE") || lexer.token() == Token.IMMEDIATE) {
                    lexer.nextToken();
                    stmt.setBuildImmediate(true);
                } else {
                    acceptIdentifier("DEFERRED");
                    stmt.setBuildDeferred(true);
                }
            } else if (identifierEquals("PARALLEL")) {
                lexer.nextToken();
                stmt.setParallel(true);
                if (lexer.token() == Token.LITERAL_INT) {
                    stmt.setParallelValue(lexer.integerValue().intValue());
                    lexer.nextToken();
                }
            } else {
                break;
            }
        }

        if (lexer.token() == Token.ENABLE) {
            lexer.nextToken();
            acceptIdentifier("QUERY");
            acceptIdentifier("REWRITE");
            stmt.setEnableQueryRewrite(true);
        }

        accept(Token.AS);
        SQLSelect select = this.createSQLSelectParser().select();
        stmt.setQuery(select);

        return stmt;
    }

    public SQLStatement parseCreateDbLink() {
        throw new ParserException("TODO " + lexer.token());
    }

    public SQLStatement parseCreateTrigger() {
        accept(Token.CREATE);

        SQLCreateTriggerStatement stmt = new SQLCreateTriggerStatement(getDbType());

        if (lexer.token() == Token.OR) {
            lexer.nextToken();
            accept(Token.REPLACE);

            stmt.setOrReplace(true);
        }

        accept(Token.TRIGGER);


        stmt.setName(this.exprParser.name());

        if (identifierEquals("BEFORE")) {
            stmt.setTriggerType(TriggerType.BEFORE);
            lexer.nextToken();
        } else if (identifierEquals("AFTER")) {
            stmt.setTriggerType(TriggerType.AFTER);
            lexer.nextToken();
        } else if (identifierEquals("INSTEAD")) {
            lexer.nextToken();
            accept(Token.OF);
            stmt.setTriggerType(TriggerType.INSTEAD_OF);
        }

        for (;;) {
            if (lexer.token() == Token.INSERT) {
                lexer.nextToken();
                stmt.getTriggerEvents().add(TriggerEvent.INSERT);
                continue;
            }

            if (lexer.token() == Token.UPDATE) {
                lexer.nextToken();
                stmt.getTriggerEvents().add(TriggerEvent.UPDATE);
                continue;
            }

            if (lexer.token() == Token.DELETE) {
                lexer.nextToken();
                stmt.getTriggerEvents().add(TriggerEvent.DELETE);
                continue;
            }
            break;
        }

        accept(Token.ON);
        stmt.setOn(this.exprParser.name());

        if (lexer.token() == Token.FOR) {
            lexer.nextToken();
            acceptIdentifier("EACH");
            accept(Token.ROW);
            stmt.setForEachRow(true);
        }

        List<SQLStatement> body = this.parseStatementList();
        if (body == null || body.isEmpty()) {
            throw new ParserException("syntax error");
        }
        stmt.setBody(body.get(0));
        return stmt;
    }

    public SQLStatement parseBlock() {
        throw new ParserException("TODO " + lexer.token());
    }

    public SQLStatement parseCreateDatabase() {
        if (lexer.token() == Token.CREATE) {
            lexer.nextToken();
        }

        accept(Token.DATABASE);

        SQLCreateDatabaseStatement stmt = new SQLCreateDatabaseStatement(getDbType());
        stmt.setName(this.exprParser.name());
        return stmt;
    }

    public SQLCreateProcedureStatement parseCreateProcedure() {
        throw new ParserException("TODO " + lexer.token());
    }

    public SQLStatement parseCreateSequence(boolean acceptCreate) {
        throw new ParserException("TODO " + lexer.token());
    }

    public SQLStatement parseCreateIndex(boolean acceptCreate) {
        if (acceptCreate) {
            accept(Token.CREATE);
        }

        SQLCreateIndexStatement stmt = new SQLCreateIndexStatement(getDbType());
        if (lexer.token() == Token.UNIQUE) {
            lexer.nextToken();
            if (identifierEquals("CLUSTERED")) {
                lexer.nextToken();
                stmt.setType("UNIQUE CLUSTERED");
            } else {
                stmt.setType("UNIQUE");
            }
        } else if (identifierEquals("FULLTEXT")) {
            stmt.setType("FULLTEXT");
            lexer.nextToken();
        } else if (identifierEquals("NONCLUSTERED")) {
            stmt.setType("NONCLUSTERED");
            lexer.nextToken();
        }

        accept(Token.INDEX);

        stmt.setName(this.exprParser.name());

        accept(Token.ON);

        stmt.setTable(this.exprParser.name());

        accept(Token.LPAREN);

        for (;;) {
            SQLSelectOrderByItem item = this.exprParser.parseSelectOrderByItem();
            item.setParent(stmt);
            stmt.addItem(item);
            if (lexer.token() == Token.COMMA) {
                lexer.nextToken();
                continue;
            }
            break;
        }
        accept(Token.RPAREN);

        return stmt;
    }

    public SQLCreateTableParser getSQLCreateTableParser() {
        return new SQLCreateTableParser(this.exprParser);
    }

    public SQLStatement parseSelect() {
        SQLSelectParser selectParser = createSQLSelectParser();
        SQLSelect select = selectParser.select();
        return new SQLSelectStatement(select,getDbType());
    }

    public SQLSelectParser createSQLSelectParser() {
        return new SQLSelectParser(this.exprParser);
    }

    public SQLUpdateStatement parseUpdateStatement() {
        SQLUpdateStatement udpateStatement = createUpdateStatement();

        if (lexer.token() == Token.UPDATE) {
            lexer.nextToken();

            SQLTableSource tableSource = this.exprParser.createSelectParser().parseTableSource();
            udpateStatement.setTableSource(tableSource);
        }

        parseUpdateSet(udpateStatement);

        if (lexer.token() == (Token.WHERE)) {
            lexer.nextToken();
            udpateStatement.setWhere(this.exprParser.expr());
        }

        return udpateStatement;
    }

    protected void parseUpdateSet(SQLUpdateStatement update) {
        accept(Token.SET);

        for (;;) {
            SQLUpdateSetItem item = this.exprParser.parseUpdateSetItem();
            update.addItem(item);

            if (lexer.token() != Token.COMMA) {
                break;
            }

            lexer.nextToken();
        }
    }

    protected SQLUpdateStatement createUpdateStatement() {
        return new SQLUpdateStatement(getDbType());
    }

    public SQLDeleteStatement parseDeleteStatement() {
        SQLDeleteStatement deleteStatement = new SQLDeleteStatement(getDbType());

        if (lexer.token() == Token.DELETE) {
            lexer.nextToken();
            if (lexer.token() == (Token.FROM)) {
                lexer.nextToken();
            }

            if (lexer.token() == Token.COMMENT) {
                lexer.nextToken();
            }

            SQLName tableName = exprParser.name();

            deleteStatement.setTableName(tableName);

            if (lexer.token() == Token.FROM) {
                lexer.nextToken();
                SQLTableSource tableSource = createSQLSelectParser().parseTableSource();
                deleteStatement.setFrom(tableSource);
            }
        }

        if (lexer.token() == (Token.WHERE)) {
            lexer.nextToken();
            SQLExpr where = this.exprParser.expr();
            deleteStatement.setWhere(where);
        }

        return deleteStatement;
    }

    public SQLCreateTableStatement parseCreateTable() {
        // SQLCreateTableParser parser = new SQLCreateTableParser(this.lexer);
        // return parser.parseCrateTable();
        throw new ParserException("TODO");
    }

    public SQLCreateViewStatement parseCreateView() {
        SQLCreateViewStatement createView = new SQLCreateViewStatement(getDbType());

        if (lexer.token() == Token.CREATE) {
            lexer.nextToken();
        }

        if (lexer.token() == Token.OR) {
            lexer.nextToken();
            accept(Token.REPLACE);
            createView.setOrReplace(true);
        }

        if (identifierEquals("ALGORITHM")) {
            lexer.nextToken();
            accept(Token.EQ);
            String algorithm = lexer.stringVal();
            createView.setAlgorithm(algorithm);
            lexer.nextToken();
        }

        if (identifierEquals("DEFINER")) {
            lexer.nextToken();
            accept(Token.EQ);
            SQLName definer = this.exprParser.name();
            createView.setDefiner(definer);
            lexer.nextToken();
        }

        if (identifierEquals("SQL")) {
            lexer.nextToken();
            acceptIdentifier("SECURITY");
            String sqlSecurity = lexer.stringVal();
            createView.setSqlSecurity(sqlSecurity);
            lexer.nextToken();
        }

        if (identifierEquals("FORCE")) {
            lexer.nextToken();
            createView.setForce(true);
        }

        this.accept(Token.VIEW);

        if (lexer.token() == Token.IF || identifierEquals("IF")) {
            lexer.nextToken();
            accept(Token.NOT);
            accept(Token.EXISTS);
            createView.setIfNotExists(true);
        }

        createView.setName(exprParser.name());

        if (lexer.token() == Token.LPAREN) {
            lexer.nextToken();

            for (;;) {

                if (lexer.token() == Token.CONSTRAINT) {
                    SQLTableConstraint constraint = (SQLTableConstraint) this.exprParser.parseConstaint();
                    createView.addColumn(constraint);
                } else {
                    SQLColumnDefinition column = new SQLColumnDefinition();
                    column.setDbType(dbType);
                    SQLName expr = this.exprParser.name();
                    column.setName(expr);

                    this.exprParser.parseColumnRest(column);

                    if (lexer.token() == Token.COMMENT) {
                        lexer.nextToken();
                        column.setComment((SQLCharExpr) exprParser.primary());
                    }

                    column.setParent(createView);
                    createView.addColumn(column);
                }

                if (lexer.token() == Token.COMMA) {
                    lexer.nextToken();
                } else {
                    break;
                }
            }

            accept(Token.RPAREN);
        }

        if (lexer.token() == Token.COMMENT) {
            lexer.nextToken();
            SQLCharExpr comment = (SQLCharExpr) exprParser.primary();
            createView.setComment(comment);
        }

        this.accept(Token.AS);

        SQLSelectParser selectParser = this.createSQLSelectParser();
        createView.setSubQuery(selectParser.select());

        if (lexer.token() == Token.WITH) {
            lexer.nextToken();

            if (identifierEquals("CASCADED")) {
                createView.setWithCascaded(true);
                lexer.nextToken();
            } else if (identifierEquals("LOCAL")){
                createView.setWithLocal(true);
                lexer.nextToken();
            } else if (identifierEquals("READ")) {
                lexer.nextToken();
                accept(Token.ONLY);
                createView.setWithReadOnly(true);
            }

            if (lexer.token() == Token.CHECK) {
                lexer.nextToken();
                createView.setWithCheckOption(true);
            }
        }

        return createView;
    }

    public SQLCommentStatement parseComment() {
        accept(Token.COMMENT);
        SQLCommentStatement stmt = new SQLCommentStatement();

        accept(Token.ON);

        if (lexer.token() == Token.TABLE) {
            stmt.setType(SQLCommentStatement.Type.TABLE);
            lexer.nextToken();
        } else if (lexer.token() == Token.COLUMN) {
            stmt.setType(SQLCommentStatement.Type.COLUMN);
            lexer.nextToken();
        }

        stmt.setOn(this.exprParser.name());

        accept(Token.IS);
        stmt.setComment(this.exprParser.expr());

        return stmt;
    }

    protected SQLAlterTableAddColumn parseAlterTableAddColumn() {
        boolean odps = JdbcConstants.ODPS.equals(dbType);

        if (odps) {
            acceptIdentifier("COLUMNS");
            accept(Token.LPAREN);
        }

        SQLAlterTableAddColumn item = new SQLAlterTableAddColumn();

        for (;;) {
            SQLColumnDefinition columnDef = this.exprParser.parseColumn();
            item.addColumn(columnDef);
            if (lexer.token() == Token.COMMA) {
                lexer.nextToken();
                if (identifierEquals("ADD")) {
                    break;
                }
                continue;
            }
            break;
        }

        if (odps) {
            accept(Token.RPAREN);
        }
        return item;
    }
    
    public SQLStatement parseStatement() {
        return parseStatement(false);
    }
    
    /**
    * @param tryBest  - 为true去解析并忽略之后的错误
    *  强制建议除非明确知道可以忽略才传tryBest=true,
    *  不然会忽略语法错误，且截断sql,导致update和delete无where条件下执行！！！
    */
    public SQLStatement parseStatement( final boolean tryBest) {
        List<SQLStatement> list = new ArrayList<SQLStatement>();
        this.parseStatementList(list, 1);
        if (tryBest) {
            if (lexer.token() != Token.EOF) {
                throw new ParserException("sql syntax error, no terminated. " + lexer.token());
            }
        }
        return list.get(0);
    }

    public SQLExplainStatement parseExplain() {
        accept(Token.EXPLAIN);
        if (identifierEquals("PLAN")) {
            lexer.nextToken();
        }

        if (lexer.token() == Token.FOR) {
            lexer.nextToken();
        }

        SQLExplainStatement explain = new SQLExplainStatement(getDbType());

        if (lexer.token == Token.HINT) {
            explain.setHints(this.exprParser.parseHints());
        }

        if (JdbcConstants.MYSQL.equals(dbType)) {
            if (identifierEquals("FORMAT")
                    || identifierEquals("EXTENDED")
                    || identifierEquals("PARTITIONS")) {
                explain.setType(lexer.stringVal);
                lexer.nextToken();
            }
        }

        explain.setStatement(parseStatement());

        return explain;
    }

    protected SQLAlterTableAddIndex parseAlterTableAddIndex() {
        SQLAlterTableAddIndex item = new SQLAlterTableAddIndex();

        if (lexer.token() == Token.UNIQUE) {
            item.setUnique(true);
            lexer.nextToken();
            if (lexer.token() == Token.INDEX) {
                lexer.nextToken();
            } else if (lexer.token() == Token.KEY) {
                item.setKey(true);
                lexer.nextToken();
            }
        } else {
            if (lexer.token() == Token.INDEX) {
                accept(Token.INDEX);
            } else if (lexer.token() == Token.KEY) {
                item.setKey(true);
                accept(Token.KEY);
            }
        }

        if (lexer.token() == Token.LPAREN) {
            lexer.nextToken();
        } else {
            item.setName(this.exprParser.name());

            if (JdbcConstants.MYSQL.equals(dbType)) {
                if (identifierEquals("USING")) {
                    lexer.nextToken();
                    String indexType = lexer.stringVal;
                    item.setType(indexType);
                    accept(Token.IDENTIFIER);
                }
            }

            accept(Token.LPAREN);
        }

        for (;;) {
            SQLSelectOrderByItem column = this.exprParser.parseSelectOrderByItem();
            item.addItem(column);
            if (lexer.token() == Token.COMMA) {
                lexer.nextToken();
                continue;
            }
            break;
        }
        accept(Token.RPAREN);
        return item;
    }

    /**
     * parse cursor open statement
     * 
     * @return
     */
    public SQLOpenStatement parseOpen() {
        SQLOpenStatement stmt = new SQLOpenStatement();
        accept(Token.OPEN);
        stmt.setCursorName(exprParser.name().getSimpleName());

        if (lexer.token() == Token.FOR) {
            lexer.nextToken();
            if (lexer.token() == Token.SELECT) {
                SQLSelectParser selectParser = createSQLSelectParser();
                SQLSelect select = selectParser.select();
                SQLQueryExpr queryExpr = new SQLQueryExpr(select);
                stmt.setFor(queryExpr);
            } else {
                throw new ParserException("TODO " + lexer.info());
            }
        }
        accept(Token.SEMI);
        stmt.setAfterSemi(true);
        return stmt;
    }

    public SQLFetchStatement parseFetch() {
        accept(Token.FETCH);

        SQLFetchStatement stmt = new SQLFetchStatement();
        stmt.setCursorName(this.exprParser.name());

        if (identifierEquals("BULK")) {
            lexer.nextToken();
            acceptIdentifier("COLLECT");
            stmt.setBulkCollect(true);
        }

        accept(Token.INTO);
        for (;;) {
            stmt.getInto().add(this.exprParser.name());
            if (lexer.token() == Token.COMMA) {
                lexer.nextToken();
                continue;
            }

            break;
        }

        return stmt;
    }

    public SQLStatement parseClose() {
        SQLCloseStatement stmt = new SQLCloseStatement();
        accept(Token.CLOSE);
        stmt.setCursorName(exprParser.name().getSimpleName());
        accept(Token.SEMI);
        stmt.setAfterSemi(true);
        return stmt;
    }

    public boolean isParseCompleteValues() {
        return parseCompleteValues;
    }

    public void setParseCompleteValues(boolean parseCompleteValues) {
        this.parseCompleteValues = parseCompleteValues;
    }

    public int getParseValuesSize() {
        return parseValuesSize;
    }

    public void setParseValuesSize(int parseValuesSize) {
        this.parseValuesSize = parseValuesSize;
    }
    
    public SQLMergeStatement parseMerge() {
        accept(Token.MERGE);

        SQLMergeStatement stmt = new SQLMergeStatement();

        parseHints(stmt.getHints());

        accept(Token.INTO);
        
        if (lexer.token() == Token.LPAREN) {
            lexer.nextToken();
            SQLSelect select = this.createSQLSelectParser().select();
            SQLSubqueryTableSource tableSource = new SQLSubqueryTableSource(select);
            stmt.setInto(tableSource);
            accept(Token.RPAREN);
        } else {
            stmt.setInto(exprParser.name());
        }
        
        stmt.setAlias(tableAlias());

        accept(Token.USING);

        SQLTableSource using = this.createSQLSelectParser().parseTableSource();
        stmt.setUsing(using);

        accept(Token.ON);
        stmt.setOn(exprParser.expr());

        boolean matched = false;
        boolean notMatched = false;

        if (lexer.token() == Token.WHEN) {
            lexer.nextToken();
            if (lexer.token() == Token.MATCHED) {
                matched = true;
                lexer.nextToken();
                stmt.setUpdateClause(parseUpdateClause());
                stmt.setUpdateClauseFirst(true);
            } else if (lexer.token() == Token.NOT) {
                lexer.nextToken();
                accept(Token.MATCHED);
                notMatched = true;
                stmt.setInsertClause(parseInsertClause());
                stmt.setUpdateClauseFirst(false);
            }
        }

        if (lexer.token() == Token.WHEN) {
            lexer.nextToken();
            if (lexer.token() == Token.MATCHED) {
                if (matched) {
                    throw new ParserException("TODO " + lexer.token());
                }
                lexer.nextToken();
                stmt.setUpdateClause(parseUpdateClause());
            } else if (lexer.token() == Token.NOT) {
                if (notMatched) {
                    throw new ParserException("TODO " + lexer.token());
                }
                lexer.nextToken();
                accept(Token.MATCHED);
                stmt.setInsertClause(parseInsertClause());
            }
        }

        SQLErrorLoggingClause errorClause = parseErrorLoggingClause();
        stmt.setErrorLoggingClause(errorClause);

        return stmt;
    }

    public SQLMergeStatement.MergeInsertClause parseInsertClause() {
        SQLMergeStatement.MergeInsertClause insertClause = new SQLMergeStatement.MergeInsertClause();

        accept(Token.THEN);
        accept(Token.INSERT);

        if (lexer.token() == Token.LPAREN) {
            accept(Token.LPAREN);
            exprParser.exprList(insertClause.getColumns(), insertClause);
            accept(Token.RPAREN);
        }
        accept(Token.VALUES);
        accept(Token.LPAREN);
        exprParser.exprList(insertClause.getValues(), insertClause);
        accept(Token.RPAREN);

        if (lexer.token() == Token.WHERE) {
            lexer.nextToken();
            insertClause.setWhere(exprParser.expr());
        }

        return insertClause;
    }

    private SQLMergeStatement.MergeUpdateClause parseUpdateClause() {
        SQLMergeStatement.MergeUpdateClause updateClause = new SQLMergeStatement.MergeUpdateClause();
        accept(Token.THEN);
        accept(Token.UPDATE);
        accept(Token.SET);

        for (;;) {
            SQLUpdateSetItem item = this.exprParser.parseUpdateSetItem();

            updateClause.addItem(item);
            item.setParent(updateClause);

            if (lexer.token() == (Token.COMMA)) {
                lexer.nextToken();
                continue;
            }

            break;
        }

        if (lexer.token() == Token.WHERE) {
            lexer.nextToken();
            updateClause.setWhere(exprParser.expr());
        }

        if (lexer.token() == Token.DELETE) {
            lexer.nextToken();
            accept(Token.WHERE);
            updateClause.setWhere(exprParser.expr());
        }

        return updateClause;
    }
    
    protected SQLErrorLoggingClause parseErrorLoggingClause() {
        if (identifierEquals("LOG")) {
            SQLErrorLoggingClause errorClause = new SQLErrorLoggingClause();

            lexer.nextToken();
            accept(Token.ERRORS);
            if (lexer.token() == Token.INTO) {
                lexer.nextToken();
                errorClause.setInto(exprParser.name());
            }

            if (lexer.token() == Token.LPAREN) {
                lexer.nextToken();
                errorClause.setSimpleExpression(exprParser.expr());
                accept(Token.RPAREN);
            }

            if (lexer.token() == Token.REJECT) {
                lexer.nextToken();
                accept(Token.LIMIT);
                errorClause.setLimit(exprParser.expr());
            }

            return errorClause;
        }
        return null;
    }
    
    public void parseHints(List<SQLHint> hints) {
        this.getExprParser().parseHints(hints);
    }

    public SQLStatement parseDescribe() {
        if (lexer.token() == Token.DESC || identifierEquals("DESCRIBE")) {
            lexer.nextToken();
        } else {
            throw new ParserException("expect DESC, actual " + lexer.token());
        }

        SQLDescribeStatement stmt = new SQLDescribeStatement();
        stmt.setDbType(dbType);

        if (identifierEquals("ROLE")) {
            lexer.nextToken();
            stmt.setObjectType(SQLObjectType.ROLE);
        } else if (identifierEquals("PACKAGE")) {
            lexer.nextToken();
            stmt.setObjectType(SQLObjectType.PACKAGE);
        } else if (identifierEquals("INSTANCE")) {
            lexer.nextToken();
            stmt.setObjectType(SQLObjectType.INSTANCE);
        }
        stmt.setObject(this.exprParser.name());

        Token token = lexer.token();
        if (token == Token.PARTITION) {
            lexer.nextToken();
            this.accept(Token.LPAREN);
            for (;;) {
                stmt.getPartition().add(this.exprParser.expr());
                if (lexer.token() == Token.COMMA) {
                    lexer.nextToken();
                    continue;
                }
                if (lexer.token() == Token.RPAREN) {
                    lexer.nextToken();
                    break;
                }
            }
        } else if (token == Token.IDENTIFIER) {
            SQLName column = this.exprParser.name();
            stmt.setColumn(column);
        }
        return stmt;
    }
}
