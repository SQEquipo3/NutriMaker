package com.javafx.nutrimaker.models;

import java.time.LocalTime;
import java.util.*;

public class DistribuidorDeCalorias {

    // Los tipos se corresponden con las categorias del catalogo de recetas.
    private static final Map<Integer, String[]> tiposComida = Map.of(
            6, new String[]{"BREAKFAST", "LUNCH", "SNACK", "LUNCH", "SNACK", "DINNER"},
            5, new String[]{"BREAKFAST", "SNACK", "LUNCH", "SNACK", "DINNER"},
            4, new String[]{"BREAKFAST", "SNACK", "LUNCH", "DINNER"},
            3, new String[]{"BREAKFAST", "LUNCH", "DINNER"},
            2, new String[]{"BREAKFAST", "LUNCH"},
            1, new String[]{"BREAKFAST"}
    );

    private static final Map<Integer, String[]> tiemposComida = Map.of(
            6, new String[]{"DESAYUNO", "ALMUERZO", "COLACION_MATUTINA", "COMIDA", "COLACION_VESPERTINA", "CENA"},
            5, new String[]{"DESAYUNO", "COLACION_MATUTINA", "COMIDA", "COLACION_VESPERTINA", "CENA"},
            4, new String[]{"DESAYUNO", "COLACION_MATUTINA", "COMIDA", "CENA"},
            3, new String[]{"DESAYUNO", "COMIDA", "CENA"},
            2, new String[]{"DESAYUNO", "COMIDA"},
            1, new String[]{"DESAYUNO"}
    );

    private static final Map<Integer, List<LocalTime>> horarios = Map.of(
            6, List.of(LocalTime.of(7, 30), LocalTime.of(10, 0), LocalTime.of(11, 30), LocalTime.of(14, 0), LocalTime.of(17, 0), LocalTime.of(20, 0)),
            5, List.of(LocalTime.of(8, 0), LocalTime.of(11, 0), LocalTime.of(14, 0), LocalTime.of(17, 0), LocalTime.of(20, 0)),
            4, List.of(LocalTime.of(8, 0), LocalTime.of(11, 0), LocalTime.of(14, 0), LocalTime.of(20, 0)),
            3, List.of(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0)),
            2, List.of(LocalTime.of(8, 0), LocalTime.of(14, 0)),
            1, List.of(LocalTime.of(8, 0))
    );

    private static final Map<Integer, Integer[]> distribucionPorcentual = Map.of(

            6, new Integer[]{
                    25, 15, 10, 25, 10, 15
            },

            5, new Integer[]{
                    30, 10, 25, 10, 25
            },

            4, new Integer[]{
                    30, 15, 35, 20
            },

            3, new Integer[]{
                    30, 40, 30
            },

            2, new Integer[]{
                    50, 50
            },

            1, new Integer[]{
                    100
            }
    );

    public static List<ComidaProgramada> distribuir(
            double caloriasTotales,
            int comidasPorDia) {

        String[] tipos = tiposComida.get(comidasPorDia);
        String[] tiempos = tiemposComida.get(comidasPorDia);
        Integer[] porcentajes = distribucionPorcentual.get(comidasPorDia);

        if (tipos == null || tiempos == null || porcentajes == null) {
            throw new IllegalArgumentException(
                    "El número de comidas debe estar entre 1 y 6."
            );
        }

        List<ComidaProgramada> resultado = new ArrayList<>();

        for (int i = 0; i < comidasPorDia; i++) {

            double caloriasAsignadas =
                    caloriasTotales * porcentajes[i] / 100.0;

            ComidaProgramada comida =
                    new ComidaProgramada(
                            tipos[i],
                            caloriasAsignadas
                    );

            resultado.add(comida);
        }

        return resultado;
    }

    public static String[] obtenerTiempos(int comidasPorDia) {
        String[] tiempos = tiemposComida.get(comidasPorDia);

        if (tiempos == null) {
            throw new IllegalArgumentException(
                    "El número de comidas debe estar entre 1 y 6."
            );
        }

        return tiempos;
    }

    public static List<LocalTime> obtenerHoras(int comidasPorDia) {
        List<LocalTime> horas = horarios.get(comidasPorDia);
        if (horas == null) {
            throw new IllegalArgumentException("El numero de comidas debe estar entre 1 y 6.");
        }
        return new ArrayList<>(horas);
    }
}
