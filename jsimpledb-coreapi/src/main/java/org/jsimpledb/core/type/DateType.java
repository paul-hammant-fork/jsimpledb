
/*
 * Copyright (C) 2015 Archie L. Cobbs. All rights reserved.
 */

package org.jsimpledb.core.type;

import com.google.common.base.Preconditions;

import java.util.Date;

import org.dellroad.stuff.string.DateEncoder;
import org.jsimpledb.util.ByteReader;
import org.jsimpledb.util.ByteWriter;
import org.jsimpledb.util.LongEncoder;
import org.jsimpledb.util.ParseContext;

/**
 * Non-null {@link Date} type. Null values are not supported by this class.
 */
public class DateType extends NonNullFieldType<Date> {

    private static final long serialVersionUID = 825120832596893074L;

    public DateType() {
        super(Date.class, 0);
    }

// FieldType

    @Override
    public Date read(ByteReader reader) {
        Preconditions.checkArgument(reader != null);
        return new Date(LongEncoder.read(reader));
    }

    @Override
    public void write(ByteWriter writer, Date date) {
        Preconditions.checkArgument(date != null, "null date");
        Preconditions.checkArgument(writer != null);
        LongEncoder.write(writer, date.getTime());
    }

    @Override
    public void skip(ByteReader reader) {
        Preconditions.checkArgument(reader != null);
        reader.skip(LongEncoder.decodeLength(reader.peek()));
    }

    @Override
    public Date fromParseableString(ParseContext ctx) {
        return DateEncoder.decode(ctx.matchPrefix(DateEncoder.PATTERN).group());
    }

    @Override
    public String toParseableString(Date date) {
        return DateEncoder.encode(date);
    }

    @Override
    public int compare(Date date1, Date date2) {
        return date1.compareTo(date2);
    }

    @Override
    public boolean hasPrefix0x00() {
        return false;
    }

    @Override
    public boolean hasPrefix0xff() {
        return false;
    }
}

