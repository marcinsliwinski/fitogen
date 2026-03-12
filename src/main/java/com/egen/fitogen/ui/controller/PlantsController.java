package com.egen.fitogen.ui.controller;

import com.egen.fitogen.domain.Plant;
import com.egen.fitogen.database.SqlitePlantRepository;
import com.egen.fitogen.repository.PlantRepository;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import javafx.fxml.FXML;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import javafx.scene.control.cell.PropertyValueFactory;

public class PlantsController {

    @FXML
    private TableView<Plant> table;

    @FXML
    private TableColumn<Plant, Integer> colId;

    @FXML
    private TableColumn<Plant, String> colSpecies;

    @FXML
    private TableColumn<Plant, String> colVariety;

    @FXML
    private TableColumn<Plant, String> colRootstock;

    @FXML
    private TableColumn<Plant, String> colLatin;

    @FXML
    private TextField searchField;

    private final PlantRepository plantRepository =
            new SqlitePlantRepository();

    private ObservableList<Plant> masterData;

    @FXML
    public void initialize() {

        colId.setCellValueFactory(
                new PropertyValueFactory<>("id")
        );

        colSpecies.setCellValueFactory(
                new PropertyValueFactory<>("species")
        );

        colVariety.setCellValueFactory(
                new PropertyValueFactory<>("variety")
        );

        colRootstock.setCellValueFactory(
                new PropertyValueFactory<>("rootstock")
        );

        colLatin.setCellValueFactory(
                new PropertyValueFactory<>("latinSpeciesName")
        );

        masterData = FXCollections.observableArrayList(
                plantRepository.findAll()
        );

        FilteredList<Plant> filteredData =
                new FilteredList<>(masterData, p -> true);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {

            filteredData.setPredicate(plant -> {

                if (newVal == null || newVal.isEmpty()) {
                    return true;
                }

                String keyword = newVal.toLowerCase();

                if (plant.getSpecies().toLowerCase().contains(keyword))
                    return true;

                if (plant.getVariety() != null &&
                        plant.getVariety().toLowerCase().contains(keyword))
                    return true;

                if (plant.getLatinSpeciesName() != null &&
                        plant.getLatinSpeciesName().toLowerCase().contains(keyword))
                    return true;

                return false;
            });

        });

        SortedList<Plant> sortedData =
                new SortedList<>(filteredData);

        sortedData.comparatorProperty()
                .bind(table.comparatorProperty());

        table.setItems(sortedData);

    }

}