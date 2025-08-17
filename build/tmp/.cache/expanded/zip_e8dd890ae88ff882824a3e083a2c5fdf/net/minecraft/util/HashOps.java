package net.minecraft.util;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.ListBuilder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.RecordBuilder.AbstractUniversalBuilder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class HashOps implements DynamicOps<HashCode> {
    private static final byte TAG_EMPTY = 1;
    private static final byte TAG_MAP_START = 2;
    private static final byte TAG_MAP_END = 3;
    private static final byte TAG_LIST_START = 4;
    private static final byte TAG_LIST_END = 5;
    private static final byte TAG_BYTE = 6;
    private static final byte TAG_SHORT = 7;
    private static final byte TAG_INT = 8;
    private static final byte TAG_LONG = 9;
    private static final byte TAG_FLOAT = 10;
    private static final byte TAG_DOUBLE = 11;
    private static final byte TAG_STRING = 12;
    private static final byte TAG_BOOLEAN = 13;
    private static final byte TAG_BYTE_ARRAY_START = 14;
    private static final byte TAG_BYTE_ARRAY_END = 15;
    private static final byte TAG_INT_ARRAY_START = 16;
    private static final byte TAG_INT_ARRAY_END = 17;
    private static final byte TAG_LONG_ARRAY_START = 18;
    private static final byte TAG_LONG_ARRAY_END = 19;
    private static final byte[] EMPTY_PAYLOAD = new byte[]{1};
    private static final byte[] FALSE_PAYLOAD = new byte[]{13, 0};
    private static final byte[] TRUE_PAYLOAD = new byte[]{13, 1};
    public static final byte[] EMPTY_MAP_PAYLOAD = new byte[]{2, 3};
    public static final byte[] EMPTY_LIST_PAYLOAD = new byte[]{4, 5};
    private static final DataResult<Object> UNSUPPORTED_OPERATION_ERROR = DataResult.error(() -> "Unsupported operation");
    private static final Comparator<HashCode> HASH_COMPARATOR = Comparator.comparingLong(HashCode::padToLong);
    private static final Comparator<Entry<HashCode, HashCode>> MAP_ENTRY_ORDER = Entry.<HashCode, HashCode>comparingByKey(HASH_COMPARATOR)
        .thenComparing(Entry.comparingByValue(HASH_COMPARATOR));
    private static final Comparator<Pair<HashCode, HashCode>> MAPLIKE_ENTRY_ORDER = Comparator.<Pair<HashCode, HashCode>, HashCode>comparing(Pair::getFirst, HASH_COMPARATOR)
        .thenComparing(Pair::getSecond, HASH_COMPARATOR);
    public static final HashOps CRC32C_INSTANCE = new HashOps(Hashing.crc32c());
    final HashFunction hashFunction;
    final HashCode empty;
    private final HashCode emptyMap;
    private final HashCode emptyList;
    private final HashCode trueHash;
    private final HashCode falseHash;

    public HashOps(HashFunction pHashFunction) {
        this.hashFunction = pHashFunction;
        this.empty = pHashFunction.hashBytes(EMPTY_PAYLOAD);
        this.emptyMap = pHashFunction.hashBytes(EMPTY_MAP_PAYLOAD);
        this.emptyList = pHashFunction.hashBytes(EMPTY_LIST_PAYLOAD);
        this.falseHash = pHashFunction.hashBytes(FALSE_PAYLOAD);
        this.trueHash = pHashFunction.hashBytes(TRUE_PAYLOAD);
    }

    public HashCode empty() {
        return this.empty;
    }

    public HashCode emptyMap() {
        return this.emptyMap;
    }

    public HashCode emptyList() {
        return this.emptyList;
    }

    public HashCode createNumeric(Number pI) {
        return switch (pI) {
            case Byte obyte -> this.createByte(obyte);
            case Short oshort -> this.createShort(oshort);
            case Integer integer -> this.createInt(integer);
            case Long olong -> this.createLong(olong);
            case Double d0 -> this.createDouble(d0);
            case Float f -> this.createFloat(f);
            default -> this.createDouble(pI.doubleValue());
        };
    }

    public HashCode createByte(byte pValue) {
        return this.hashFunction.newHasher(2).putByte((byte)6).putByte(pValue).hash();
    }

    public HashCode createShort(short pValue) {
        return this.hashFunction.newHasher(3).putByte((byte)7).putShort(pValue).hash();
    }

    public HashCode createInt(int pValue) {
        return this.hashFunction.newHasher(5).putByte((byte)8).putInt(pValue).hash();
    }

    public HashCode createLong(long pValue) {
        return this.hashFunction.newHasher(9).putByte((byte)9).putLong(pValue).hash();
    }

    public HashCode createFloat(float pValue) {
        return this.hashFunction.newHasher(5).putByte((byte)10).putFloat(pValue).hash();
    }

    public HashCode createDouble(double pValue) {
        return this.hashFunction.newHasher(9).putByte((byte)11).putDouble(pValue).hash();
    }

    public HashCode createString(String pValue) {
        return this.hashFunction.newHasher().putByte((byte)12).putInt(pValue.length()).putUnencodedChars(pValue).hash();
    }

    public HashCode createBoolean(boolean pValue) {
        return pValue ? this.trueHash : this.falseHash;
    }

    private static Hasher hashMap(Hasher pHasher, Map<HashCode, HashCode> pMap) {
        pHasher.putByte((byte)2);
        pMap.entrySet()
            .stream()
            .sorted(MAP_ENTRY_ORDER)
            .forEach(p_392920_ -> pHasher.putBytes(p_392920_.getKey().asBytes()).putBytes(p_392920_.getValue().asBytes()));
        pHasher.putByte((byte)3);
        return pHasher;
    }

    static Hasher hashMap(Hasher pHasher, Stream<Pair<HashCode, HashCode>> pMap) {
        pHasher.putByte((byte)2);
        pMap.sorted(MAPLIKE_ENTRY_ORDER).forEach(p_392939_ -> pHasher.putBytes(p_392939_.getFirst().asBytes()).putBytes(p_392939_.getSecond().asBytes()));
        pHasher.putByte((byte)3);
        return pHasher;
    }

    public HashCode createMap(Stream<Pair<HashCode, HashCode>> pMap) {
        return hashMap(this.hashFunction.newHasher(), pMap).hash();
    }

    public HashCode createMap(Map<HashCode, HashCode> pMap) {
        return hashMap(this.hashFunction.newHasher(), pMap).hash();
    }

    public HashCode createList(Stream<HashCode> pInput) {
        Hasher hasher = this.hashFunction.newHasher();
        hasher.putByte((byte)4);
        pInput.forEach(p_397145_ -> hasher.putBytes(p_397145_.asBytes()));
        hasher.putByte((byte)5);
        return hasher.hash();
    }

    public HashCode createByteList(ByteBuffer pInput) {
        Hasher hasher = this.hashFunction.newHasher();
        hasher.putByte((byte)14);
        hasher.putBytes(pInput);
        hasher.putByte((byte)15);
        return hasher.hash();
    }

    public HashCode createIntList(IntStream pInput) {
        Hasher hasher = this.hashFunction.newHasher();
        hasher.putByte((byte)16);
        pInput.forEach(hasher::putInt);
        hasher.putByte((byte)17);
        return hasher.hash();
    }

    public HashCode createLongList(LongStream pInput) {
        Hasher hasher = this.hashFunction.newHasher();
        hasher.putByte((byte)18);
        pInput.forEach(hasher::putLong);
        hasher.putByte((byte)19);
        return hasher.hash();
    }

    public HashCode remove(HashCode pInput, String pKey) {
        return pInput;
    }

    @Override
    public RecordBuilder<HashCode> mapBuilder() {
        return new HashOps.MapHashBuilder();
    }

    @Override
    public ListBuilder<HashCode> listBuilder() {
        return new HashOps.ListHashBuilder();
    }

    @Override
    public String toString() {
        return "Hash " + this.hashFunction;
    }

    public <U> U convertTo(DynamicOps<U> pOps, HashCode pHashCode) {
        throw new UnsupportedOperationException("Can't convert from this type");
    }

    public Number getNumberValue(HashCode pHashCode, Number pNumber) {
        return pNumber;
    }

    public HashCode set(HashCode pInput, String pKey, HashCode pValue) {
        return pInput;
    }

    public HashCode update(HashCode pInput, String pKey, Function<HashCode, HashCode> pUpdater) {
        return pInput;
    }

    public HashCode updateGeneric(HashCode pInput, HashCode pKey, Function<HashCode, HashCode> pUpdater) {
        return pInput;
    }

    private static <T> DataResult<T> unsupported() {
        return (DataResult<T>)UNSUPPORTED_OPERATION_ERROR;
    }

    public DataResult<HashCode> get(HashCode pInput, String pKey) {
        return unsupported();
    }

    public DataResult<HashCode> getGeneric(HashCode pInput, HashCode pKey) {
        return unsupported();
    }

    public DataResult<Number> getNumberValue(HashCode pInput) {
        return unsupported();
    }

    public DataResult<Boolean> getBooleanValue(HashCode pInput) {
        return unsupported();
    }

    public DataResult<String> getStringValue(HashCode pInput) {
        return unsupported();
    }

    public DataResult<HashCode> mergeToList(HashCode pList, HashCode pValue) {
        return unsupported();
    }

    public DataResult<HashCode> mergeToList(HashCode pList, List<HashCode> pValues) {
        return unsupported();
    }

    public DataResult<HashCode> mergeToMap(HashCode pList, HashCode pKey, HashCode pValue) {
        return unsupported();
    }

    public DataResult<HashCode> mergeToMap(HashCode pList, Map<HashCode, HashCode> pValues) {
        return unsupported();
    }

    public DataResult<HashCode> mergeToMap(HashCode pList, MapLike<HashCode> pValues) {
        return unsupported();
    }

    public DataResult<Stream<Pair<HashCode, HashCode>>> getMapValues(HashCode pInput) {
        return unsupported();
    }

    public DataResult<Consumer<BiConsumer<HashCode, HashCode>>> getMapEntries(HashCode pInput) {
        return unsupported();
    }

    public DataResult<Stream<HashCode>> getStream(HashCode pInput) {
        return unsupported();
    }

    public DataResult<Consumer<Consumer<HashCode>>> getList(HashCode pInput) {
        return unsupported();
    }

    public DataResult<MapLike<HashCode>> getMap(HashCode pInput) {
        return unsupported();
    }

    public DataResult<ByteBuffer> getByteBuffer(HashCode pInput) {
        return unsupported();
    }

    public DataResult<IntStream> getIntStream(HashCode pInput) {
        return unsupported();
    }

    public DataResult<LongStream> getLongStream(HashCode pInput) {
        return unsupported();
    }

    class ListHashBuilder extends AbstractListBuilder<HashCode, Hasher> {
        public ListHashBuilder() {
            super(HashOps.this);
        }

        protected Hasher initBuilder() {
            return HashOps.this.hashFunction.newHasher().putByte((byte)4);
        }

        protected Hasher append(Hasher p_394930_, HashCode p_392558_) {
            return p_394930_.putBytes(p_392558_.asBytes());
        }

        protected DataResult<HashCode> build(Hasher p_394248_, HashCode p_391292_) {
            assert p_391292_.equals(HashOps.this.empty);

            p_394248_.putByte((byte)5);
            return DataResult.success(p_394248_.hash());
        }
    }

    final class MapHashBuilder extends AbstractUniversalBuilder<HashCode, List<Pair<HashCode, HashCode>>> {
        public MapHashBuilder() {
            super(HashOps.this);
        }

        protected List<Pair<HashCode, HashCode>> initBuilder() {
            return new ArrayList<>();
        }

        protected List<Pair<HashCode, HashCode>> append(HashCode p_391659_, HashCode p_392968_, List<Pair<HashCode, HashCode>> p_394869_) {
            p_394869_.add(Pair.of(p_391659_, p_392968_));
            return p_394869_;
        }

        protected DataResult<HashCode> build(List<Pair<HashCode, HashCode>> p_396177_, HashCode p_392314_) {
            assert p_392314_.equals(HashOps.this.empty());

            return DataResult.success(HashOps.hashMap(HashOps.this.hashFunction.newHasher(), p_396177_.stream()).hash());
        }
    }
}