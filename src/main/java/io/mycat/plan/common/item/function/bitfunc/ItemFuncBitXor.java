package io.mycat.plan.common.item.function.bitfunc;

import java.math.BigInteger;
import java.util.List;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;

import io.mycat.plan.common.field.Field;
import io.mycat.plan.common.item.Item;
import io.mycat.plan.common.item.function.primary.ItemFuncBit;

public class ItemFuncBitXor extends ItemFuncBit {

	public ItemFuncBitXor(Item a, Item b) {
		super(a, b);
	}

	@Override
	public final String funcName() {
		return "^";
	}

	@Override
	public BigInteger valInt() {
		BigInteger arg1 = args.get(0).valInt();
		BigInteger arg2 = args.get(1).valInt();
		if (nullValue = (args.get(0).nullValue || args.get(1).nullValue))
			return BigInteger.ZERO;
		return arg1.xor(arg2);
	}

	@Override
	public SQLExpr toExpression() {
		return new SQLBinaryOpExpr(args.get(0).toExpression(), SQLBinaryOperator.BitwiseXor, args.get(1).toExpression());
	}

	@Override
	protected Item cloneStruct(boolean forCalculate, List<Item> calArgs, boolean isPushDown, List<Field> fields) {
		List<Item> newArgs = null;
		if (!forCalculate)
			newArgs = cloneStructList(args);
		else
			newArgs = calArgs;
		return new ItemFuncBitXor(newArgs.get(0), newArgs.get(1));
	}
}