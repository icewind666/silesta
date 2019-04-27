/*
 * This file is generated by jOOQ.
 */
package com.silesta.models.tables;


import com.silesta.models.Indexes;
import com.silesta.models.Keys;
import com.silesta.models.Public;
import com.silesta.models.tables.records.BankOperationsRecord;

import java.sql.Date;
import java.util.Arrays;
import java.util.List;

import javax.annotation.processing.Generated;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.11.11"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class BankOperations extends TableImpl<BankOperationsRecord> {

    private static final long serialVersionUID = 1548075748;

    /**
     * The reference instance of <code>public.bank_operations</code>
     */
    public static final BankOperations BANK_OPERATIONS = new BankOperations();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<BankOperationsRecord> getRecordType() {
        return BankOperationsRecord.class;
    }

    /**
     * The column <code>public.bank_operations.amount</code>.
     */
    public final TableField<BankOperationsRecord, Float> AMOUNT = createField("amount", org.jooq.impl.SQLDataType.REAL.nullable(false).defaultValue(org.jooq.impl.DSL.field("0", org.jooq.impl.SQLDataType.REAL)), this, "");

    /**
     * The column <code>public.bank_operations.desc</code>.
     */
    public final TableField<BankOperationsRecord, String> DESC = createField("desc", org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * The column <code>public.bank_operations.ext_cat_id</code>.
     */
    public final TableField<BankOperationsRecord, Long> EXT_CAT_ID = createField("ext_cat_id", org.jooq.impl.SQLDataType.BIGINT, this, "");

    /**
     * The column <code>public.bank_operations.cat_name</code>.
     */
    public final TableField<BankOperationsRecord, String> CAT_NAME = createField("cat_name", org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * The column <code>public.bank_operations.is_income</code>.
     */
    public final TableField<BankOperationsRecord, Boolean> IS_INCOME = createField("is_income", org.jooq.impl.SQLDataType.BOOLEAN, this, "");

    /**
     * The column <code>public.bank_operations.id</code>.
     */
    public final TableField<BankOperationsRecord, Integer> ID = createField("id", org.jooq.impl.SQLDataType.INTEGER.nullable(false).defaultValue(org.jooq.impl.DSL.field("nextval('bank_operations_id_seq'::regclass)", org.jooq.impl.SQLDataType.INTEGER)), this, "");

    /**
     * The column <code>public.bank_operations.op_date</code>.
     */
    public final TableField<BankOperationsRecord, Date> OP_DATE = createField("op_date", org.jooq.impl.SQLDataType.DATE, this, "");

    /**
     * The column <code>public.bank_operations.source</code>.
     */
    public final TableField<BankOperationsRecord, String> SOURCE = createField("source", org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * Create a <code>public.bank_operations</code> table reference
     */
    public BankOperations() {
        this(DSL.name("bank_operations"), null);
    }

    /**
     * Create an aliased <code>public.bank_operations</code> table reference
     */
    public BankOperations(String alias) {
        this(DSL.name(alias), BANK_OPERATIONS);
    }

    /**
     * Create an aliased <code>public.bank_operations</code> table reference
     */
    public BankOperations(Name alias) {
        this(alias, BANK_OPERATIONS);
    }

    private BankOperations(Name alias, Table<BankOperationsRecord> aliased) {
        this(alias, aliased, null);
    }

    private BankOperations(Name alias, Table<BankOperationsRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""));
    }

    public <O extends Record> BankOperations(Table<O> child, ForeignKey<O, BankOperationsRecord> key) {
        super(child, key, BANK_OPERATIONS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema getSchema() {
        return Public.PUBLIC;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.PK);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Identity<BankOperationsRecord, Integer> getIdentity() {
        return Keys.IDENTITY_BANK_OPERATIONS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<BankOperationsRecord> getPrimaryKey() {
        return Keys.PK;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<BankOperationsRecord>> getKeys() {
        return Arrays.<UniqueKey<BankOperationsRecord>>asList(Keys.PK);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BankOperations as(String alias) {
        return new BankOperations(DSL.name(alias), this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BankOperations as(Name alias) {
        return new BankOperations(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public BankOperations rename(String name) {
        return new BankOperations(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public BankOperations rename(Name name) {
        return new BankOperations(name, null);
    }
}
