package moze_intel.projecte.emc.generators;

import com.google.common.collect.Maps;
import moze_intel.projecte.emc.arithmetics.LongFraction;

import java.util.Map;

public class FractionToLongGenerator<T> implements IValueGenerator<T, Long>
{
	private final IValueGenerator<T, LongFraction> inner;

	public FractionToLongGenerator(IValueGenerator<T, LongFraction> inner)
	{
		this.inner = inner;
	}

	@Override
	public Map<T, Long> generateValues()
	{
		Map<T, LongFraction> innerResult = inner.generateValues();
		Map<T, Long> myResult = Maps.newHashMap();
		for (Map.Entry<T, LongFraction> entry : innerResult.entrySet())
		{
			long value = entry.getValue().longValue();
			if (value > 0)
			{
				myResult.put(entry.getKey(), value);
			}
		}
		return myResult;
	}
}