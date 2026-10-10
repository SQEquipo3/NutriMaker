package com.javafx.nutrimaker.models;

import java.time.LocalTime;
import java.util.*;

public class DistribuidorDeCalorias {

    private static final Map<Integer, String[]> tiposComida = Map.of(
            6, new String[]{
                    "BREAKFAST",
                    "SNACK",
                    "LUNCH",
                    "LUNCH",
                    "SNACK",
                    "DINNER"
            },

            5, new String[]{
                    "BREAKFAST",
                    "SNACK",
                    "LUNCH",
                    "SNACK",
                    "DINNER"
            },

            4, new String[]{
                    "BREAKFAST",
                    "LUNCH",
                    "LUNCH",
                    "SNACK"
            },

            3, new String[]{
                    "BREAKFAST",
                    "LUNCH",
                    "DINNER"
            },

            2, new String[]{
                    "BREAKFAST",
                    "LUNCH"
            },

            1, new String[]{
                    "BREAKFAST"
            }
    );

    private static final Map<Integer, String[]> tiemposComida = Map.of(
            6, new String[]{
                    "DESAYUNO",
                    "COLACION_MATUTINA",
                    "ALMUERZO",
                    "COMIDA",
                    "COLACION_VESPERTINA",
                    "CENA"
            },

            5, new String[]{
                    "DESAYUNO",
                    "COLACION_MATUTINA",
                    "COMIDA",
                    "COLACION_VESPERTINA",
                    "CENA"
            },

            4, new String[]{
                    "DESAYUNO",
                    "ALMUERZO",
                    "COMIDA",
                    "COLACION_VESPERTINA"
            },

            3, new String[]{
                    "DESAYUNO",
                    "COMIDA",
                    "CENA"
            },

            2, new String[]{
                    "DESAYUNO",
                    "COMIDA"
            },

            1, new String[]{
                    "DESAYUNO"
            }
    );

    private static final Map<Integer, Integer[]> distribucionPorcentual = Map.of(

            6, new Integer[]{
                    25, 10, 12, 22, 10, 21
            },

            5, new Integer[]{
                    30, 10, 25, 10, 25
            },

            4, new Integer[]{
                    25, 25, 30, 20
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

        if (comidasPorDia < 1 || comidasPorDia > 6) {
            throw new IllegalArgumentException(
                    "El número de comidas por día debe estar entre 1 y 6."
            );
        }

        LocalTime inicio = LocalTime.of(8, 0);
        LocalTime fin = LocalTime.of(20, 0);

        int minutosTotales =
                (fin.toSecondOfDay() - inicio.toSecondOfDay()) / 60;

        int intervalo =
                minutosTotales /
                        (comidasPorDia - 1 > 0
                                ? comidasPorDia - 1
                                : 1);

        List<LocalTime> horarios = new ArrayList<>();

        for (int i = 0; i < comidasPorDia; i++) {

            LocalTime horaComida =
                    inicio.plusMinutes(intervalo * i);

            horarios.add(horaComida);
        }

        return horarios;
    }
}