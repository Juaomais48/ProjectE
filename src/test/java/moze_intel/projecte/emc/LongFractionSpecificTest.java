package moze_intel.projecte.emc;

import moze_intel.projecte.emc.arithmetics.IValueArithmetic;
import moze_intel.projecte.emc.arithmetics.LongFraction;
import moze_intel.projecte.emc.arithmetics.LongFractionArithmetic;
import moze_intel.projecte.emc.collector.IExtendedMappingCollector;
import moze_intel.projecte.emc.collector.LongToFractionCollector;
import moze_intel.projecte.emc.generators.FractionToLongGenerator;
import moze_intel.projecte.emc.generators.IValueGenerator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class LongFractionSpecificTest
{
	@Rule
	public Timeout timeout = new Timeout(3000);

	public IValueGenerator<String, Long> valueGenerator;
	public IExtendedMappingCollector<String, Long, IValueArithmetic<LongFraction>> mappingCollector;

	@Before
	public void setup()
	{
		SimpleGraphMapper<String, LongFraction, IValueArithmetic<LongFraction>> mapper = new SimpleGraphMapper<String, LongFraction, IValueArithmetic<LongFraction>>(new LongFractionArithmetic());
		valueGenerator = new FractionToLongGenerator<String>(mapper);
		mappingCollector = new LongToFractionCollector<String, IValueArithmetic<LongFraction>>(mapper);
	}

	@Test
	public void exploitableFractionCycleTerminates()
	{
		mappingCollector.setValueBefore("a", 1L);
		mappingCollector.addConversion(1, "exploitable", Arrays.asList("a"));
		mappingCollector.addConversion(2, "exploitable", Arrays.asList("exploitable"));

		Map<String, Long> values = valueGenerator.generateValues();
		assertEquals(1L, getValue(values, "a"));
		assertEquals(0L, getValue(values, "exploitable"));
	}

	private static long getValue(Map<String, Long> map, String key)
	{
		Long value = map.get(key);
		return value == null ? 0 : value;
	}
}