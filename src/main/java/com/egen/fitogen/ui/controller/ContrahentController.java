package com.egen.fitogen.ui.controller;

import com.egen.fitogen.domain.Contrahent;
import com.egen.fitogen.database.SqliteContrahentRepository;
import com.egen.fitogen.repository.ContrahentRepository;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import javafx.fxml.FXML;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import javafx.scene.control.cell.PropertyValueFactory;

public class ContrahentController {

    @FXML
    private TableView<Contrahent> table;

    @FXML
    private TableColumn<Contrahent, Integer> colId;

    @FXML
    private TableColumn<Contrahent, String> colName;

    @FXML
    private TableColumn<Contrahent, String> colCountry;

    @FXML
    private TableColumn<Contrahent, String> colCity;

    @FXML
    private TableColumn<Contrahent, String> colPhyto;

    @FXML
    private TextField searchField;

    private final ContrahentRepository repository =
            new SqliteContrahentRepository();

    private ObservableList<Contrahent> masterData;

    @FXML
    public void initialize() {

        colId.setCellValueFactory(
                new PropertyValueFactory<>("id")
        );

        colName.setCellValueFactory(
                new PropertyValueFactory<>("name")
        );

        colCountry.setCellValueFactory(
                new PropertyValueFactory<>("country")
        );

        colCity.setCellValueFactory(
                new PropertyValueFactory<>("city")
        );

        colPhyto.setCellValueFactory(
                new PropertyValueFactory<>("phytosanitaryNumber")
        );

        masterData = FXCollections.observableArrayList(
                repository.findAll()
        );

        FilteredList<Contrahent> filtered =
                new FilteredList<>(masterData, p -> true);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {

            filtered.setPredicate(c -> {

                if (newVal == null || newVal.isEmpty())
                    return true;

                String keyword = newVal.toLowerCase();

                if (c.getName().toLowerCase().contains(keyword))
                    return true;

                if (c.getCountry().toLowerCase().contains(keyword))
                    return true;

                if (c.getCity().toLowerCase().contains(keyword))
                    return true;

                return false;

            });

        });

        SortedList<Contrahent> sorted =
                new SortedList<>(filtered);

        sorted.comparatorProperty()
                .bind(table.comparatorProperty());

        table.setItems(sorted);

    }

}