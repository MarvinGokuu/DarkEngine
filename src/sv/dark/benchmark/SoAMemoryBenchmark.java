package sv.dark.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import sv.dark.scene.DarkTransformSoA;
import sv.dark.memory.SectorMemoryVault;
import sv.dark.ecs.DarkScene;
import sv.dark.ecs.DarkEntity;

import java.util.concurrent.TimeUnit;
import java.util.Random;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class SoAMemoryBenchmark {

    private static final int ENTITY_COUNT = 100_000;

    // --- OOP (Array of Structs) ---
    private static class OOPTransform {
        double x, y, z;
        public OOPTransform(double x, double y, double z) {
            this.x = x; this.y = y; this.z = z;
        }
    }
    private OOPTransform[] oopArray;

    // --- Data-Oriented (Struct of Arrays) ---
    private DarkTransformSoA soa;

    // --- Hybrid ECS Abstraction (OOP over SoA) ---
    private DarkScene scene;
    private DarkEntity[] facadeArray;

    @Setup
    public void setup() {
        // Init OOP
        oopArray = new OOPTransform[ENTITY_COUNT];
        Random rand = new Random(42);
        for (int i = 0; i < ENTITY_COUNT; i++) {
            oopArray[i] = new OOPTransform(rand.nextDouble(), rand.nextDouble(), rand.nextDouble());
        }

        // Init SoA Off-Heap
        soa = new DarkTransformSoA(ENTITY_COUNT);
        for (int i = 0; i < ENTITY_COUNT; i++) {
            soa.setEntity(i, rand.nextDouble(), rand.nextDouble(), rand.nextDouble(), 0, 0, 0);
        }

        // Init Hybrid ECS
        scene = new DarkScene(ENTITY_COUNT);
        facadeArray = new DarkEntity[ENTITY_COUNT];
        for (int i = 0; i < ENTITY_COUNT; i++) {
            DarkEntity entity = scene.spawnEntity();
            entity.setPosition(rand.nextDouble(), rand.nextDouble(), rand.nextDouble());
            entity.setVelocity(0, 0, 0);
            facadeArray[i] = entity;
        }
    }

    @Benchmark
    public void testAoS_OOP(Blackhole bh) {
        double sum = 0;
        for (int i = 0; i < ENTITY_COUNT; i++) {
            OOPTransform t = oopArray[i];
            // Simulate physics translation
            t.x += 1.0;
            t.y += 1.0;
            t.z += 1.0;
            sum += t.x + t.y + t.z;
        }
        bh.consume(sum);
    }

    @Benchmark
    public void testSoA_DataOriented(Blackhole bh) {
        double sum = 0;
        for (int i = 0; i < ENTITY_COUNT; i++) {
            long offset64 = i * 8L;
            // Simulate physics translation (Read from Off-Heap, Add, Write to Off-Heap)
            double x = soa.globalPosX.get(java.lang.foreign.ValueLayout.JAVA_DOUBLE, offset64) + 1.0;
            double y = soa.globalPosY.get(java.lang.foreign.ValueLayout.JAVA_DOUBLE, offset64) + 1.0;
            double z = soa.globalPosZ.get(java.lang.foreign.ValueLayout.JAVA_DOUBLE, offset64) + 1.0;
            
            soa.globalPosX.set(java.lang.foreign.ValueLayout.JAVA_DOUBLE, offset64, x);
            soa.globalPosY.set(java.lang.foreign.ValueLayout.JAVA_DOUBLE, offset64, y);
            soa.globalPosZ.set(java.lang.foreign.ValueLayout.JAVA_DOUBLE, offset64, z);
            
            sum += x + y + z;
        }
        bh.consume(sum);
    }

    @Benchmark
    public void testHybrid_DarkEntity(Blackhole bh) {
        double sum = 0;
        for (int i = 0; i < ENTITY_COUNT; i++) {
            DarkEntity e = facadeArray[i];
            // Simulate physics translation
            double x = e.getPositionX() + 1.0;
            double y = e.getPositionY() + 1.0;
            double z = e.getPositionZ() + 1.0;
            
            e.setPosition(x, y, z);
            sum += x + y + z;
        }
        bh.consume(sum);
    }

    @TearDown
    public void teardown() {
        soa.destroy();
        scene.getSoA().destroy();
    }
}
