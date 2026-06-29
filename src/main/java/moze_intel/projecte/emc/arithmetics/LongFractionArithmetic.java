package moze_intel.projecte.emc.arithmetics;

public class LongFractionArithmetic implements IValueArithmetic<LongFraction>
{
	@Override
	public boolean isZero(LongFraction value)
	{
		return LongFraction.ZERO.equals(value);
	}

	@Override
	public LongFraction getZero()
	{
		return LongFraction.ZERO;
	}

	@Override
	public LongFraction add(LongFraction a, LongFraction b)
	{
		if (isFree(a)) return b;
		if (isFree(b)) return a;
		return a.add(b);
	}

	@Override
	public LongFraction mul(int a, LongFraction b)
	{
		if (isFree(b)) return getFree();
		return b.multiply(a);
	}

	@Override
	public LongFraction div(LongFraction a, int b)
	{
		if (isFree(a)) return getFree();
		LongFraction result = a.divide(b);
		if (LongFraction.ZERO.compareTo(result) <= 0 && result.compareTo(LongFraction.ONE) < 0)
		{
			return result;
		}
		return LongFraction.of(result.longValue());
	}

	@Override
	public LongFraction getFree()
	{
		return LongFraction.FREE;
	}

	@Override
	public boolean isFree(LongFraction value)
	{
		return LongFraction.FREE.equals(value);
	}
}