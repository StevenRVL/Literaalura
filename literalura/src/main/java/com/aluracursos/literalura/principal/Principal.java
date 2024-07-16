package com.aluracursos.literalura.principal;

import com.aluracursos.literalura.modelos.*;
import com.aluracursos.literalura.repositorio.AutorRepository;
import com.aluracursos.literalura.repositorio.LibroRepository;
import com.aluracursos.literalura.service.ConsumoAPI;
import com.aluracursos.literalura.service.ConvierteDatos;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Scanner;

@Component
public class Principal {

    private static final String URL_BASE = "https://gutendex.com/books/";
    private ConsumoAPI consumoAPI = new ConsumoAPI();
    private ConvierteDatos conversor = new ConvierteDatos();
    private Scanner entradaDeUsuario = new Scanner(System.in);

    private LibroRepository repositoryLibro;
    private AutorRepository repositoryAutor;


    public Principal(LibroRepository libro, AutorRepository autor ){
        this.repositoryLibro = libro;
        this.repositoryAutor = autor;
    }

    public void muestraElMenu(){
    int opcion = 0;
        while (opcion != 9) {
        String menu = """
                    ***************************************************
                      *****  Bienvenido a la biblioteca ALURA   *****
                    ***************************************************
                    
                    1 - Busqueda de libro por titulo
                    2 - Mostrar listado de libros
                    3 - Busqueda de autores por año 
                    4 - Busqueda de libro por Idioma
                    5 - Top 10 libros más descargados
                    
                    9 - Salir
                    \n""";

        System.out.println(menu);
            while (!entradaDeUsuario.hasNextInt()) {
                System.out.println("Formato inválido, ingrese un número que esté disponible en el menú!");
                entradaDeUsuario.nextLine();
            }
            opcion = entradaDeUsuario.nextInt();
            entradaDeUsuario.nextLine();

        switch (opcion) {
            case 1 -> buscaLibro();
            case 2 -> muestraListaDeLibrosGuardados();
            case 3 -> buscaAutorPorAnio();
            case 4 -> buscaLibroPorIdioma();
            case 5 -> muestraTop10LibrosMasDescargados();
            case 9 -> {

                System.out.println("Saliendo de la aplicación\n");
                System.exit(9);
            }
            default -> System.out.println("Opción no valida intenta otra vez");
        }
    }
}

    private Datos buscarDatosLibros() {
        System.out.println("Ingrese el nombre del libro que desea buscar: ");
        String libro = entradaDeUsuario.nextLine();
        String json = consumoAPI.obtenerDatos(URL_BASE +"?search=" + libro.replace(" ", "+"));
        return conversor.obtenerDatos(json, Datos.class);
    }

    private void buscaLibro() {
        Datos datos = buscarDatosLibros();

        if (!datos.resultados().isEmpty()) {
            DatosLibros datosLibros = datos.resultados().get(0);
            DatosAutor datosAutor = datosLibros.autor().get(0);
            System.out.println("Título: " + datosLibros.titulo());
            System.out.println("Autor: " + datosAutor.nombre());
            Autor autor = new Autor(datosAutor);
            repositoryAutor.save(autor);
            repositoryLibro.save(new Libro(datosLibros, autor ));
        } else {
            System.out.println("Este libro no se encuentra en nuestra biblioteca intenta con otro");
        }
    }

    @Transactional
    private void muestraListaDeLibrosGuardados() {
        try {
            List<Libro> libros = repositoryLibro.findAll();
            if (libros.isEmpty()) {
                System.out.println("No encontramos ningun libro en la base de datos.");
            } else {
                libros.forEach(libro -> {
                    System.out.println("EL Título del libro es: " + libro.getTitulo());
                    System.out.println("El nombre del Autor es: " + libro.getAutor().getNombre());
                    System.out.println("El total de descargas es: " + libro.getNumeroDeDescargas());
                    System.out.println("Idiomas que dispone el libro son: " + String.join(", ", libro.getIdiomas())+"\n");
                });
            }
        } catch (Exception e) {
            System.out.println("Hubo un problema al motrar listas.");
            e.printStackTrace();
        }
    }



    private void buscaAutorPorAnio() {
        System.out.println("Ingrese la fecha de busqueda: ");
        while (!entradaDeUsuario.hasNextInt()) {
            System.out.println("Respuesta no valida, intenta de nuevo.");
            entradaDeUsuario.nextLine();
        }
        int fechaBusqueda = entradaDeUsuario.nextInt();
        entradaDeUsuario.nextLine();
        String anioString = String.valueOf(fechaBusqueda);

        List<Autor> autoresVivos = repositoryAutor.autorVivoEnDeterminadoAnio(anioString);
        if (autoresVivos.isEmpty()) {
            System.out.println("No encontramos  autores vivos en ese año");
        } else {
            System.out.println("----- Autores vivos en " + fechaBusqueda + " -----");
            autoresVivos.forEach(System.out::println);
            System.out.println("----------------------------------------\n");
        }
    }



    private void buscaLibroPorIdioma() {
        System.out.println("""
                -Elija el idioma que desea  consultar:
                 
                1) Español (ES)
                2) Inglés (EN)
                3) Menú principal
         
                """);
        int opcion = entradaDeUsuario.nextInt();
        entradaDeUsuario.nextLine();
        List<Libro> libros;
        switch (opcion) {
            case 1:
                libros = repositoryLibro.findByIdiomasContains("es");
                if (!libros.isEmpty()) {
                    libros.forEach(System.out::println);
                } else {
                    System.out.println("No encontramos libros regristrados en Español.");
                }
                break;
            case 2:
                libros = repositoryLibro.findByIdiomasContains("en");
                if (!libros.isEmpty()) {
                    libros.forEach(System.out::println);
                } else {
                    System.out.println("No encontramos libros regristrados en Inglés.");
                }
                break;
            case 3:
                muestraElMenu();
                break;
            default:
                System.out.println("Opción no válida.");
        }
    }

    private void muestraTop10LibrosMasDescargados() {
        String json = consumoAPI.obtenerDatos(URL_BASE );
        Datos datos = conversor.obtenerDatos(json, Datos.class);
        if (!datos.resultados().isEmpty()) {
            System.out.println("----- Top 10 Libros Más Descargados -----");
            for (int i = 0; i < Math.min(10, datos.resultados().size()); i++) {
                DatosLibros datosLibros = datos.resultados().get(i);
                System.out.println("El título es: " + datosLibros.titulo());
                System.out.println("El autor es: " + datosLibros.autor().get(0).nombre());
                System.out.println("Los idiomas son: " + datosLibros.idiomas().get(0));
                System.out.println("El número de descargas es: " + datosLibros.numeroDeDescargas());
                System.out.println("----------------------------------------");
            }
        } else {
            System.out.println("No se encontraron libros en el top 10 de descargas.");
        }
    }

}