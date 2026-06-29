package moze_intel.projecte.emc.collector;

import moze_intel.projecte.emc.arithmetics.IValueArithmetic;
import moze_intel.projecte.emc.arithmetics.LongFraction;

import java.util.Map;

public class LongToFractionCollector<T, A extends IValueArithmetic> extends AbstractMappingCollector<T, Long, A>
{
	IExtendedMappingCollector<T, LongFraction, A> inner;

	public LongToFractionCollector(IExtendedMappingCollector<T, LongFraction, A> inner)
	{
		super(inner.getArithmetic());
		this.inner = inner;
	}

	@Override
	public void setValueFromConversion(int outnumber, T something, Map<T, Integer> ingredientsWithAmount)
	{
		inner.setValueFromConversion(outnumber, something, ingredientsWithAmount);
	}

	@Override
	public void addConversion(int outnumber, T output, Map<T, Integer> ingredientsWithAmount, A arithmeticForConversion)
	{
		inner.addConversion(outnumber, output, ingredientsWithAmount, arithmeticForConversion);
	}

	@Override
	public void setValueBefore(T something, Long value)
	{
		inner.setValueBefore(something, LongFraction.of(value));
	}

	@Override
	public void setValueAfter(T something, Long value)
	{
		inner.setValueAfter(something, LongFraction.of(value));
	}
}