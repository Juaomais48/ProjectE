package moze_intel.projecte.emc.arithmetics;

import java.math.BigInteger;

public final class LongFraction implements Comparable<LongFraction>
{
	private static final BigInteger MAX_DENOMINATOR = BigInteger.valueOf(Long.MAX_VALUE);
	public static final LongFraction ZERO = new LongFraction(BigInteger.ZERO, BigInteger.ONE);
	public static final LongFraction ONE = new LongFraction(BigInteger.ONE, BigInteger.ONE);
	public static final LongFraction FREE = new LongFraction(BigInteger.valueOf(Long.MIN_VALUE), BigInteger.ONE);

	private final BigInteger numerator;
	private final BigInteger denominator;

	private LongFraction(BigInteger numerator, BigInteger denominator)
	{
		if (denominator.signum() == 0)
		{
			throw new ArithmeticException("denominator must not be zero");
		}

		if (denominator.signum() < 0)
		{
			numerator = numerator.negate();
			denominator = denominator.negate();
		}

		BigInteger gcd = numerator.gcd(denominator);
		numerator = numerator.divide(gcd);
		denominator = denominator.divide(gcd);
		if (denominator.compareTo(MAX_DENOMINATOR) > 0 && numerator.abs().compareTo(denominator) < 0)
		{
			numerator = BigInteger.ZERO;
			denominator = BigInteger.ONE;
		}
		this.numerator = numerator;
		this.denominator = denominator;
	}

	public static LongFraction of(long value)
	{
		if (value == Long.MIN_VALUE)
		{
			return FREE;
		}
		return new LongFraction(BigInteger.valueOf(value), BigInteger.ONE);
	}

	public LongFraction add(LongFraction other)
	{
		return new LongFraction(numerator.multiply(other.denominator).add(other.numerator.multiply(denominator)), denominator.multiply(other.denominator));
	}

	public LongFraction multiply(int value)
	{
		return new LongFraction(numerator.multiply(BigInteger.valueOf(value)), denominator);
	}

	public LongFraction divide(int value)
	{
		return new LongFraction(numerator, denominator.multiply(BigInteger.valueOf(value)));
	}

	public long longValue()
	{
		BigInteger value = numerator.divide(denominator);
		if (value.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0)
		{
			return Long.MAX_VALUE;
		}
		if (value.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0)
		{
			return Long.MIN_VALUE;
		}
		return value.longValue();
	}

	@Override
	public int compareTo(LongFraction other)
	{
		return numerator.multiply(other.denominator).compareTo(other.numerator.multiply(denominator));
	}

	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof LongFraction))
		{
			return false;
		}
		LongFraction other = (LongFraction) obj;
		return numerator.equals(other.numerator) && denominator.equals(other.denominator);
	}

	@Override
	public int hashCode()
	{
		return numerator.hashCode() * 31 + denominator.hashCode();
	}

	@Override
	public String toString()
	{
		if (denominator.equals(BigInteger.ONE))
		{
			return numerator.toString();
		}
		return numerator + "/" + denominator;
	}
}