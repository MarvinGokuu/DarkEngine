// Reading Order: 00100022
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.ecs;

/**
 * ECS Component Marker Interface (Phase 30).
 * 
 * Todas las clases (o records) que representen un Componente en el ECS
 * deben implementar esta interfaz para ser gestionadas de forma pura
 * por el ComponentArray y el DarkScene.
 */
public interface DarkComponent {
    // Interfaz marcadora sin métodos.
    // Futuro: Soportará Valhalla Inline Types (Java 26).
}
