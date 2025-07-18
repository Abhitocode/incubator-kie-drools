/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.drools.traits.core.factmodel;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;

import org.drools.base.factmodel.traits.Thing;
import org.drools.base.factmodel.traits.TraitType;
import org.drools.base.factmodel.traits.TraitTypeMap;

public class TraitTypeMapImpl<T extends String, K extends Thing<C>, C>
        extends TypeHierarchy<K, BitMaskKey<K>>
        implements Map<String, K>, Externalizable,
                   TraitTypeMap<T, K, C> {

    private Map<String,K> innerMap;

    private BitSet currentTypeCode = new BitSet();
    private transient Collection<K> mostSpecificTraits = new LinkedList<>();


    private static final BitSet NO_STATIC = new BitSet();
    private BitSet staticTypeCode;
    private Map<String,BitSet> staticTypes;

    public TraitTypeMapImpl() {
    }

    public TraitTypeMapImpl(Map map) {
        innerMap = map;

        // create "top" element placeholder. will be replaced by a Thing proxy later, should the core object don it
        ThingProxyImplPlaceHolder thingPlaceHolder = ThingProxyImplPlaceHolder.getThingPlaceHolder();
        addMember( (K) thingPlaceHolder, thingPlaceHolder._getTypeCode() );
    }

    public int size() {
        return innerMap.size();
    }

    public boolean isEmpty() {
        return innerMap.isEmpty();
    }

    public boolean containsKey(Object key) {
        return innerMap.containsKey( key );
    }

    public boolean containsValue(Object value) {
        return innerMap.containsValue( value );
    }

    public K get( Object key ) {
        return innerMap.get( key );
    }

    public K put( String key, K value ) {
        BitSet code = ((TraitType) value)._getTypeCode();

        addMember( value, code );
        innerMap.put( key, value );

        currentTypeCode.or( code );
        mostSpecificTraits = null;

        return value;
    }

    @Override
    public void setBottomCode( BitSet code ) {
        if ( ! hasKey(code) ) {
            super.setBottomCode(code);
            addMember( (K) new NullTraitType( code ), code );
        }
    }

    @Override
    protected BitMaskKey<K> wrap( K value, BitSet key ) {
        return new BitMaskKey<>( System.identityHashCode( value ), value );
    }

    @Override
    public K putSafe( String key, K value ) {
        BitSet code = ((TraitType) value)._getTypeCode();

        addMember( value, code );
        innerMap.put( key, value );

        currentTypeCode.or( code );
        mostSpecificTraits = null;

        return value;
    }


    public K remove( Object key ) {
        K t = innerMap.remove( key );
        if ( t instanceof TraitProxyImpl) {
            ((TraitProxyImpl) t).shed();
        }
        removeMember( ( (TraitProxyImpl) t )._getTypeCode() );

        mostSpecificTraits = null;
        resetCurrentCode();
        return t;
    }

    @Override
    public Collection<K> removeCascade( String traitName ) {
        if ( ! innerMap.containsKey( traitName ) ) {
            if ( staticTypes != null ) {
                BitSet staticCode = staticTypes.get( traitName );
                if ( staticCode != null ) {
                    return removeCascade( staticTypes.get( traitName ) );
                }
            }
            return Collections.emptyList();
        }
        K thing = innerMap.get( traitName );
        return removeCascade( ( (TraitType) thing )._getTypeCode() );
    }

    @Override
    public Collection<K> removeCascade( BitSet code ) {
        Collection<K> subs = this.lowerDescendants( code );
        List<K> ret = new ArrayList<>( subs.size() );
        for ( K k : subs ) {
            Key<K> t = new BitMaskKey<>( System.identityHashCode(k), k );
            TraitType tt = (TraitType) t.getValue();
            if ( ! tt._isVirtual() ) {
                ret.add( t.getValue() );
                removeMember( tt._getTypeCode() );
                K thing = innerMap.remove( tt._getTraitName() );
                if ( thing instanceof TraitProxyImpl) {
                    ((TraitProxyImpl) thing).shed(); //is this working?
                }
            }
        }

        mostSpecificTraits = null;
        resetCurrentCode();
        return ret;
    }

    private void resetCurrentCode() {
        currentTypeCode = new BitSet( currentTypeCode.length() );
        if ( staticTypeCode != null && staticTypeCode != NO_STATIC ) {
            currentTypeCode.or( staticTypeCode );
        }
        if ( ! this.values().isEmpty() ) {
            for ( Thing x : this.values() ) {
                currentTypeCode.or( ((TraitType) x)._getTypeCode() );
            }
        }
    }

    public void putAll( Map<? extends String, ? extends K> m ) {
        for ( K proxy : m.values() ) {
            addMember( proxy, ((TraitProxyImpl) proxy)._getTypeCode() );
        }
        resetCurrentCode();
        mostSpecificTraits = null;
        innerMap.putAll( m );
    }

    public void clear() {
        innerMap.clear();
    }

    public Set<String> keySet() {
        return innerMap.keySet();
    }

    public Collection<K> values() {
        return innerMap.values();
    }

    public Set<Entry<String, K>> entrySet() {
        return innerMap.entrySet();
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("TraitTypeMap{");
        for (Entry<String,K> trtEntry : innerMap.entrySet()) {
            Object proxy = trtEntry.getValue();
            bldr.append(trtEntry.getKey()).append(" : ").append(proxy.getClass().getName()).append("@").append(System.identityHashCode(proxy)).append("; ");
        }
        bldr.append("}");
        return bldr.toString();
    }

    public void writeExternal( ObjectOutput objectOutput ) throws IOException {
        super.writeExternal( objectOutput );

        objectOutput.writeInt( innerMap.size() );
        List<String> keys = new ArrayList<>( innerMap.keySet() );
        Collections.sort( keys );
        for ( String k : keys ) {
            objectOutput.writeObject( k );
            objectOutput.writeObject( innerMap.get( k ) );
        }

        objectOutput.writeObject( currentTypeCode );
        objectOutput.writeObject( staticTypeCode );
        objectOutput.writeObject( mostSpecificTraits );
    }

    public void readExternal( ObjectInput objectInput ) throws IOException, ClassNotFoundException {
        super.readExternal( objectInput );

        innerMap = new HashMap<>();
        int n = objectInput.readInt();
        for ( int j = 0; j < n; j++ ) {
            String k = (String) objectInput.readObject();
            K tf = (K) objectInput.readObject();
            innerMap.put( k, tf );
        }

        currentTypeCode = (BitSet) objectInput.readObject();
        staticTypeCode = (BitSet) objectInput.readObject();
        mostSpecificTraits = (Collection<K>) objectInput.readObject();
    }


    public Collection<K> getMostSpecificTraits() {
        if ( getBottomCode() == null ) {
            // not yet initialized -> no trait donned yet
            return null;
        }
        if( mostSpecificTraits != null && ! mostSpecificTraits.isEmpty() ) {
            return mostSpecificTraits;
        }
        if ( hasKey( getBottomCode() ) ) {
            K b = getMember( getBottomCode() );
            if ( ((TraitType) b)._isVirtual() ) {
                mostSpecificTraits = immediateParents( getBottomCode() );
                return mostSpecificTraits;
            } else {
                mostSpecificTraits = Collections.singleton( b );
                return mostSpecificTraits;
            }
        } else {
            mostSpecificTraits = immediateParents( getBottomCode() );
            return mostSpecificTraits;
        }
    }

    @Override
    public BitSet getCurrentTypeCode() {
        return currentTypeCode;
    }

    public BitSet getStaticTypeCode() {
        return staticTypeCode;
    }

    public void setStaticTypeCode( BitSet staticTypeCode ) {
        if ( staticTypeCode != null ) {
            this.staticTypeCode = staticTypeCode;
            currentTypeCode.or( staticTypeCode );
        } else {
            this.staticTypeCode = NO_STATIC;
        }
    }

    public void addStaticTrait( String name, BitSet code ) {
        if ( staticTypes == null ) {
            staticTypes = new HashMap<>();
        }
        staticTypes.put( name, code );
    }
}
