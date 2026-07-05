// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.test;

import sv.dark.ecs.DarkEntity;
import sv.dark.ecs.DarkScene;
import sv.dark.scene.DarkKinematicsSystem;
import sv.dark.scene.DarkTransformSoA;

public class SceneGraphHierarchyTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  PHASE 2: SCENE GRAPH & TOPOLOGICAL SORT TEST  ");
        System.out.println("==================================================");

        try {
            // Mock setup for testing (No RHI needed for CPU logic)
            // We just need a DarkScene
            DarkScene scene = new DarkScene(100);
            
            // Create Entities
            System.out.println("[1] Spawning Entities...");
            DarkEntity root = scene.spawnEntity();
            DarkEntity childA = scene.spawnEntity();
            DarkEntity childB = scene.spawnEntity();
            DarkEntity grandChild = scene.spawnEntity();
            
            root.setPosition(10.0, 0.0, 0.0);
            childA.setPosition(5.0, 0.0, 0.0); // Local to root
            childB.setPosition(0.0, 5.0, 0.0); // Local to root
            grandChild.setPosition(2.0, 0.0, 0.0); // Local to childA
            
            // Setup Hierarchy
            System.out.println("[2] Setting up Hierarchy (Scrambled Order)...");
            grandChild.setParent(childA);
            childB.setParent(root);
            childA.setParent(root);
            
            // Topological Sort
            System.out.println("[3] Executing Topological Sort...");
            long t0 = System.nanoTime();
            scene.topologicalSort();
            long t1 = System.nanoTime();
            System.out.println("Sort took: " + (t1 - t0) + " ns");
            
            // Validate Physical Order (Parent must appear before Child)
            System.out.println("[4] Validating Physical Memory Order...");
            int pRoot = scene.getPhysicalIndex(root.getId());
            int pChildA = scene.getPhysicalIndex(childA.getId());
            int pChildB = scene.getPhysicalIndex(childB.getId());
            int pGrandChild = scene.getPhysicalIndex(grandChild.getId());
            
            if (pRoot > pChildA || pRoot > pChildB) throw new RuntimeException("Root must be before Children!");
            if (pChildA > pGrandChild) throw new RuntimeException("ChildA must be before GrandChild!");
            System.out.println("Order Validation: PASSED");
            
            // Warmup JIT to prevent DarkKinematicsSystem Hard-Limit (15ms) from aborting the first cold execution
            System.out.println("[4.5] Warming up JIT Compiler...");
            DarkTransformSoA soa = scene.getSoA();
            for (int i = 0; i < 500; i++) {
                DarkKinematicsSystem.update(soa, 0.0f, 0.0, 0.0, 0.0);
            }
            
            // Reset positions after warmup in case they drifted
            root.setPosition(10.0, 0.0, 0.0);
            childA.setPosition(5.0, 0.0, 0.0);
            childB.setPosition(0.0, 5.0, 0.0);
            grandChild.setPosition(2.0, 0.0, 0.0);
            
            // Execute Kinematics
            System.out.println("[5] Executing Kinematics System...");
            DarkKinematicsSystem.update(soa, 1.0f, 0.0, 0.0, 0.0);
            
            // Validate Global Positions
            System.out.println("[6] Validating Global Positions...");
            
            // Root: local(10,0,0) -> global(10,0,0)
            if (Math.abs(root.getPositionX() - 10.0) > 0.001) throw new RuntimeException("Root X mismatch: " + root.getPositionX());
            
            // ChildA: local(5,0,0) + root(10,0,0) = global(15,0,0)
            if (Math.abs(childA.getPositionX() - 15.0) > 0.001) throw new RuntimeException("ChildA X mismatch: " + childA.getPositionX());
            
            // GrandChild: local(2,0,0) + childA(15,0,0) = global(17,0,0)
            if (Math.abs(grandChild.getPositionX() - 17.0) > 0.001) throw new RuntimeException("GrandChild X mismatch: " + grandChild.getPositionX());
            
            System.out.println("Global Position Validation: PASSED");
            
            scene.destroy();
            System.out.println("==================================================");
            System.out.println("               ALL TESTS PASSED                   ");
            System.out.println("==================================================");
            
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
