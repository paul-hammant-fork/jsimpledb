
/*
 * Copyright (C) 2015 Archie L. Cobbs. All rights reserved.
 */

package org.jsimpledb;

import com.google.common.base.Converter;
import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

import java.lang.reflect.Method;
import java.util.List;

import org.jsimpledb.change.ListFieldAdd;
import org.jsimpledb.change.ListFieldClear;
import org.jsimpledb.change.ListFieldRemove;
import org.jsimpledb.change.ListFieldReplace;
import org.jsimpledb.schema.ListSchemaField;

/**
 * Represents a list field in a {@link JClass}.
 */
public class JListField extends JCollectionField {

    JListField(JSimpleDB jdb, String name, int storageId, JSimpleField elementField, String description,
               Method getter, Method setter) {
        super(jdb, name, storageId, elementField, description, getter, setter);
    }

    @Override
    public List<?> getValue(JObject jobj) {
        Preconditions.checkArgument(jobj != null, "null jobj");
        return jobj.getTransaction().readListField(jobj.getObjId(), this.storageId, false);
    }

    @Override
    public <R> R visit(JFieldSwitch<R> target) {
        return target.caseJListField(this);
    }

    @Override
    ListSchemaField toSchemaItem(JSimpleDB jdb) {
        final ListSchemaField schemaField = new ListSchemaField();
        super.initialize(jdb, schemaField);
        return schemaField;
    }

    @Override
    ListElementIndexInfo toIndexInfo(JSimpleField subField) {
        assert subField == this.elementField;
        return new ListElementIndexInfo(this);
    }

    @Override
    @SuppressWarnings("serial")
    <E> TypeToken<List<E>> buildTypeToken(TypeToken<E> elementType) {
        return new TypeToken<List<E>>() { }.where(new TypeParameter<E>() { }, elementType);
    }

    // This method exists solely to bind the generic type parameters
    @Override
    @SuppressWarnings("serial")
    <T, E> void addChangeParameterTypes(List<TypeToken<?>> types, Class<T> targetType, TypeToken<E> elementType) {
        types.add(new TypeToken<ListFieldAdd<T, E>>() { }
          .where(new TypeParameter<T>() { }, targetType)
          .where(new TypeParameter<E>() { }, elementType.wrap()));
        types.add(new TypeToken<ListFieldClear<T>>() { }
          .where(new TypeParameter<T>() { }, targetType));
        types.add(new TypeToken<ListFieldRemove<T, E>>() { }
          .where(new TypeParameter<T>() { }, targetType)
          .where(new TypeParameter<E>() { }, elementType.wrap()));
        types.add(new TypeToken<ListFieldReplace<T, E>>() { }
          .where(new TypeParameter<T>() { }, targetType)
          .where(new TypeParameter<E>() { }, elementType.wrap()));
    }

    @Override
    public ListConverter<?, ?> getConverter(JTransaction jtx) {
        final Converter<?, ?> elementConverter = this.elementField.getConverter(jtx);
        return elementConverter != null ? this.createConverter(elementConverter) : null;
    }

    // This method exists solely to bind the generic type parameters
    private <X, Y> ListConverter<X, Y> createConverter(Converter<X, Y> elementConverter) {
        return new ListConverter<>(elementConverter);
    }

// Bytecode generation

    @Override
    Method getFieldReaderMethod() {
        return ClassGenerator.JTRANSACTION_READ_LIST_FIELD_METHOD;
    }
}

