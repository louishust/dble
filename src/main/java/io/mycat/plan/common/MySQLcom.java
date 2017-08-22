package io.mycat.plan.common;

import io.mycat.plan.common.field.FieldUtil;
import io.mycat.plan.common.item.FieldTypes;
import io.mycat.plan.common.item.Item;
import io.mycat.plan.common.item.Item.ItemResult;
import io.mycat.plan.common.item.Item.ItemType;
import io.mycat.plan.common.ptr.BoolPtr;
import io.mycat.plan.common.time.MySQLTime;
import io.mycat.plan.common.time.MySQLTimeStatus;
import io.mycat.plan.common.time.MySQLTimestampType;
import io.mycat.plan.common.time.MyTime;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

public class MySQLcom {
    public static final double M_PI = Math.PI;
    public static final int DBL_DIG = 6;
    public static final int FLT_DIG = 10;
    public static final int DECIMAL_LONGLONG_DIGITS = 22;

    /**
     * maximum length of buffer in our big digits (uint32).
     */
    public static final int DECIMAL_BUFF_LENGTH = 9;

    /* the number of digits that my_decimal can possibly contain */
    public static final int DECIMAL_MAX_POSSIBLE_PRECISION = (DECIMAL_BUFF_LENGTH * 9);

    /**
     * maximum guaranteed precision of number in decimal digits (number of our
     * digits * number of decimal digits in one our big digit - number of
     * decimal digits in one our big digit decreased by 1 (because we always put
     * decimal point on the border of our big digits))
     */
    public static final int DECIMAL_MAX_PRECISION = (DECIMAL_MAX_POSSIBLE_PRECISION - 8 * 2);

    public static final int DECIMAL_MAX_SCALE = 30;
    public static final int DECIMAL_NOT_SPECIFIED = 31;

    public static final BigInteger BI64BACK = new BigInteger("18446744073709551616");

    public static String setInt(long num, boolean unsignedFlag) {
        return String.valueOf(num);
    }

    public static String setReal(double num, int decimals) {
        BigDecimal bd = new BigDecimal(num);
        bd = bd.setScale(decimals);
        double db = bd.doubleValue();
        return String.valueOf(db);
    }

    public static BigDecimal int2Decimal(long num, boolean unsigned) {
        return BigDecimal.valueOf(num);
    }

    public static BigDecimal double2Decimal(double num, int decimals) {
        BigDecimal bd = new BigDecimal(num);
        return bd.setScale(decimals);
    }

    public static double str2Double(String str) {
        try {
            double db = Double.parseDouble(str);
            return db;
        } catch (Exception e) {
            return 0.0;
        }
    }

    public static long str2Long(String str) {
        try {
            long l = Long.parseLong(str);
            return l;
        } catch (Exception e) {
            return 0;
        }
    }

    public static BigDecimal str2Decimal(String str) {
        try {
            double db = Double.parseDouble(str);
            BigDecimal bd = BigDecimal.valueOf(db);
            return bd;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * str转换成longlong
     *
     * @param cs    str的byte数组
     * @param start 起始位置，包含
     * @param end   结束位置，包含
     * @param error
     * @return
     */
    public static BigInteger my_strtoll10(char[] cs, int start, int end, BoolPtr error) {
        String tmp = new String(cs, start, end - start + 1);
        error.set(false);
        try {
            BigInteger bi = new BigInteger(tmp);
            return bi;
        } catch (Exception e) {
            error.set(true);
            return BigInteger.ZERO;
        }
    }

    /**
     * binary compare, num need to <= b1.size && <= b2.size
     *
     * @param b1
     * @param b2
     * @param num
     * @return
     */
    public static int memcmp(byte[] b1, byte[] b2, int num) {
        for (int i = 0; i < num; i++) {
            if (b1[i] < b2[i]) {
                return -1;
            } else if (b1[i] == b2[i]) {
                continue;
            } else {
                return 1;
            }
        }
        return 0;
    }

    public static BigInteger getUnsignedLong(long l) {
        BigInteger bi = BigInteger.valueOf(l);
        BigInteger bmask = new BigInteger("FFFFFFFFFFFFFFFF", 16);
        return bi.and(bmask);
    }

    /**
     * @return converted value. 0 on error and on zero-dates -- check 'failure'
     * @brief Convert date provided in a string to its packed temporal int
     * representation.
     * @param[in] thd thread handle
     * @param[in] str a string to convert
     * @param[in] warn_type type of the timestamp for issuing the warning
     * @param[in] warn_name field name for issuing the warning
     * @param[out] error_arg could not extract a DATE or DATETIME
     * @details Convert date provided in the string str to the int
     * representation. If the string contains wrong date or doesn't
     * contain it at all then a warning is issued. The warn_type and
     * the warn_name arguments are used as the name and the type of the
     * field when issuing the warning.
     */
    public static long get_date_from_str(String str, MySQLTimestampType warntype, BoolPtr error) {
        MySQLTime ltime = new MySQLTime();
        MySQLTimeStatus status = new MySQLTimeStatus();
        error.set(MyTime.str_to_datetime(str, str.length(), ltime, MyTime.TIME_FUZZY_DATE, status));
        if (error.get())
            return 0;
        return MyTime.TIME_to_longlong_datetime_packed(ltime);

    }

    /*
     * Collects different types for comparison of first item with each other
     * items
     *
     * SYNOPSIS collect_cmp_types() items Array of items to collect types from
     * nitems Number of items in the array skip_nulls Don't collect types of
     * NULL items if TRUE
     *
     * DESCRIPTION This function collects different result types for comparison
     * of the first item in the list with each of the remaining items in the
     * 'items' array.
     *
     * RETURN 0 - if row type incompatibility has been detected (see
     * cmp_row_type) Bitmap of collected types - otherwise 可以表示出一共有几种type
     */
    public static int collect_cmp_types(List<Item> items, boolean skipnulls) {
        int foundtypes = 0;
        ItemResult leftResult = items.get(0).resultType();
        for (int i = 1; i < items.size(); i++) {
            if (skipnulls && items.get(i).type() == Item.ItemType.NULL_ITEM)
                continue;
            if (leftResult == ItemResult.ROW_RESULT || items.get(i).resultType() == ItemResult.ROW_RESULT
                    && cmpRowType(items.get(0), items.get(i)) != 0)
                return 0;
            foundtypes |= 1 << MySQLcom.item_cmp_type(leftResult, items.get(i).resultType()).ordinal();
        }
        /*
         * Even if all right-hand items are NULLs and we are skipping them all,
         * we need at least one type bit in the found_type bitmask.
         */
        if (skipnulls && foundtypes == 0)
            foundtypes |= 1 << leftResult.ordinal();
        return foundtypes;
    }

    public static int cmpRowType(Item item1, Item item2) {
        // TODO
        return 0;
    }

    public static ItemResult item_cmp_type(ItemResult a, ItemResult b) {

        if (a == ItemResult.STRING_RESULT && b == ItemResult.STRING_RESULT)
            return ItemResult.STRING_RESULT;
        if (a == ItemResult.INT_RESULT && b == ItemResult.INT_RESULT)
            return ItemResult.INT_RESULT;
        if ((a == ItemResult.INT_RESULT || a == ItemResult.DECIMAL_RESULT)
                && (b == ItemResult.INT_RESULT || b == ItemResult.DECIMAL_RESULT))
            return ItemResult.DECIMAL_RESULT;
        return ItemResult.REAL_RESULT;
    }

    public static FieldTypes agg_field_type(List<Item> items, int startIndex, int nitems) {
        if (nitems == 0 || items.get(startIndex).resultType() == ItemResult.ROW_RESULT)
            return FieldTypes.valueOf("-1");
        FieldTypes res = items.get(startIndex).fieldType();
        for (int i = 1; i < nitems; i++)
            res = FieldUtil.field_type_merge(res, items.get(startIndex + i).fieldType());
        return res;
    }

    public static ItemResult agg_result_type(List<Item> items, int startIndex, int size) {
        ItemResult type = ItemResult.STRING_RESULT;
        /* Skip beginning NULL items */
        int index = 0, index_end;
        Item item;
        for (index = startIndex, index_end = startIndex + size; index < index_end; index++) {
            item = items.get(index);
            if (item.type() != ItemType.NULL_ITEM) {
                type = item.resultType();
                index++;
                break;
            }
        }
        /* Combine result types. Note: NULL items don't affect the result */
        for (; index < index_end; index++) {
            item = items.get(index);
            if (item.type() != ItemType.NULL_ITEM)
                type = item_store_type(type, item);
        }
        return type;
    }

    public static ItemResult item_store_type(ItemResult a, Item item) {
        ItemResult b = item.resultType();

        if (a == ItemResult.STRING_RESULT || b == ItemResult.STRING_RESULT)
            return ItemResult.STRING_RESULT;
        else if (a == ItemResult.REAL_RESULT || b == ItemResult.REAL_RESULT)
            return ItemResult.REAL_RESULT;
        else if (a == ItemResult.DECIMAL_RESULT || b == ItemResult.DECIMAL_RESULT)
            return ItemResult.DECIMAL_RESULT;
        else
            return ItemResult.INT_RESULT;
    }

    public static byte[] long2Byte(BigInteger bi) {
        long x = -1;
        if (bi.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0)
            x = bi.subtract(BI64BACK).longValue();
        else
            x = bi.longValue();
        int retLen = -1;
        byte[] bb = new byte[8];
        int index = -1;
        bb[++index] = (byte) (x >> 56);
        if (bb[index] != 0 && retLen == -1)
            retLen = 8;
        bb[++index] = (byte) (x >> 48);
        if (bb[index] != 0 && retLen == -1)
            retLen = 7;
        bb[++index] = (byte) (x >> 40);
        if (bb[index] != 0 && retLen == -1)
            retLen = 6;
        bb[++index] = (byte) (x >> 32);
        if (bb[index] != 0 && retLen == -1)
            retLen = 5;
        bb[++index] = (byte) (x >> 24);
        if (bb[index] != 0 && retLen == -1)
            retLen = 4;
        bb[++index] = (byte) (x >> 16);
        if (bb[index] != 0 && retLen == -1)
            retLen = 3;
        bb[++index] = (byte) (x >> 8);
        if (bb[index] != 0 && retLen == -1)
            retLen = 2;
        bb[++index] = (byte) (x >> 0);
        if (retLen == -1)
            retLen = 1;
        return Arrays.copyOfRange(bb, bb.length - retLen, bb.length);
    }

    public static int memcmp(byte[] a_ptr, byte[] b_ptr) {
        int a_len = a_ptr.length, b_len = b_ptr.length;
        if (a_len >= b_len)
            return memcmp0(a_ptr, b_ptr);
        else
            return -memcmp(b_ptr, a_ptr);
    }

    public static void memcpy(byte[] a_ptr, int a_start, byte[] b_ptr) {
        assert (a_ptr.length - a_start + 1 == b_ptr.length);
        for (int i = 0; i < b_ptr.length; i++) {
            a_ptr[a_start + i] = b_ptr[i];
        }
    }

    /**
     * 比较两个byte数组的大小，其中a_ptr的长度>=b_ptr的长度
     *
     * @param a_ptr
     * @param b_ptr
     * @return
     */
    private static int memcmp0(byte[] a_ptr, byte[] b_ptr) {
        int a_len = a_ptr.length, b_len = b_ptr.length;
        for (int i = 0; i < a_len - b_len; i++) {
            if (a_ptr[i] != 0) // a比b多出了值
                return 1;
        }
        int a_start = a_len - b_len;
        for (int i = 0; i < b_len; i++) {
            byte a_byte = a_ptr[a_start + i];
            byte b_byte = b_ptr[i];
            if (a_byte > b_byte)
                return 1;
            else if (a_byte < b_byte)
                return -1;
        }
        return 0;
    }

    /**
     * 解析rowpacket时使用，rowpacket的数据全都是字符串
     *
     * @param charsetIndex
     * @param buff
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String getFullString(String charsetName, byte[] buff) throws UnsupportedEncodingException {
        if (buff == null || charsetName == null)
            return null;
        if (Charset.isSupported(charsetName)) {
            return new String(buff, charsetName);
        } else {
            String msg = "unsupported character set :" + charsetName;
            throw new UnsupportedEncodingException(msg);
        }
    }

    public static long[] log_10_int = new long[]{1, 10, 100, 1000, 10000L, 100000L, 1000000L, 10000000L, 100000000L,
            1000000000L, 10000000000L, 100000000000L, 1000000000000L, 10000000000000L, 100000000000000L,
            1000000000000000L, 10000000000000000L, 100000000000000000L, 1000000000000000000L};

    public static long pow10(int index) {
        return (long) Math.pow(10, index);
    }

    public static final String NULLS = null;

    public static int check_word(String[] nameArray, char[] cs, int offset, int count) {
        String val = new String(cs, offset, count);
        for (int index = 0; index < nameArray.length; index++) {
            if (val.equalsIgnoreCase(nameArray[index]))
                return index;
        }
        return 0;
    }
}
