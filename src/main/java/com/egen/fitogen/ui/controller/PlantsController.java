package com.egen.fitogen.ui.controller;

import com.egen.fitogen.domain.Plant;
import com.egen.fitogen.repository.PlantRepository;
import com.egen.fitogen.database.SqlitePlantRepository;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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

    private final PlantRepository plantRepository =
            new SqlitePlantRepository();

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

        refresh();
    }

    private void refresh() {

        table.setItems(
                FXCollections.observableArrayList(
                        plantRepository.findAll()
                )
        );

    }

}