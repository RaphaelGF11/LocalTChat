import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SettingsWindow extends JDialog {
    private List<Parameter> parameters;
    private List<Parameter> originalParameters;

    public SettingsWindow(Frame owner, List<Parameter> parameters) {
        super(owner, "Paramètres", true); // Le troisième paramètre rend la fenêtre modale
        this.parameters = parameters;
        this.originalParameters = copyParameters(parameters);

        setSize(400, 300);
        setLocationRelativeTo(owner);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        for (Parameter param : parameters) {
            JPanel paramPanel = new JPanel(new BorderLayout());
            paramPanel.setPreferredSize(new Dimension(380, 25)); // Diviser la hauteur d'un paramètre par 2

            JLabel label = new JLabel(param.getLabel());
            label.setPreferredSize(new Dimension(266, 25)); // 70% de la largeur totale
            paramPanel.add(label, BorderLayout.WEST);

            JPanel valuePanel = new JPanel(new BorderLayout());
            valuePanel.setPreferredSize(new Dimension(95, 25)); // 25% de la largeur totale
            valuePanel.setLayout(new GridBagLayout()); // Utiliser GridBagLayout pour centrer le contenu

            switch (param.getType()) {
                case "String":
                    JTextField textField = new JTextField(param.getValue().toString());
                    textField.addActionListener(e -> param.setValue(textField.getText()));
                    textField.setHorizontalAlignment(JTextField.CENTER);
                    textField.setPreferredSize(new Dimension(95, 25)); // Occuper l'intégralité des 25%
                    valuePanel.add(textField);
                    break;
                case "Boolean":
                    JCheckBox checkBox = new JCheckBox();
                    checkBox.setSelected((Boolean) param.getValue());
                    checkBox.addActionListener(e -> param.setValue(checkBox.isSelected()));
                    valuePanel.add(checkBox);
                    break;
                case "Int":
                    JSpinner spinner = new JSpinner(new SpinnerNumberModel((int) param.getValue(), Integer.MIN_VALUE, Integer.MAX_VALUE, 1));
                    spinner.addChangeListener(e -> param.setValue(spinner.getValue()));
                    spinner.setPreferredSize(new Dimension(95, 25)); // Occuper l'intégralité des 25%
                    valuePanel.add(spinner);
                    break;
                // Ajoutez ici d'autres types de paramètres si nécessaire
                default:
                    throw new IllegalArgumentException("Type non pris en charge: " + param.getType());
            }

            paramPanel.add(Box.createHorizontalStrut(20), BorderLayout.CENTER); // 5% d'espace vide
            paramPanel.add(valuePanel, BorderLayout.EAST);

            panel.add(paramPanel);
        }

        add(new JScrollPane(panel), BorderLayout.CENTER);

        // Ajouter les boutons en bas
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3));
        JButton applyButton = new JButton("Appliquer");
        JButton cancelButton = new JButton("Annuler");
        JButton saveButton = new JButton("Enregistrer");

        applyButton.addActionListener(e -> applyChanges());
        cancelButton.addActionListener(e -> {
            cancelChanges();
            dispose();
        });
        saveButton.addActionListener(e -> {
            applyChanges();
            saveChanges();
            dispose();
        });

        buttonPanel.add(applyButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

        add(buttonPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void applyChanges() {
        // Appliquez les changements aux paramètres
        for (Parameter param : parameters) {
            System.out.println("Appliquer: " + param.getName() + " = " + param.getValue());
        }
    }

    private void cancelChanges() {
        // Réinitialisez les paramètres à leurs valeurs d'origine
        for (int i = 0; i < parameters.size(); i++) {
            parameters.get(i).setValue(originalParameters.get(i).getValue());
        }
    }

    private void saveChanges() {
        // Enregistrez les changements si nécessaire
        System.out.println("Enregistrer les paramètres.");
    }

    private List<Parameter> copyParameters(List<Parameter> parameters) {
        List<Parameter> copy = new ArrayList<>();
        for (Parameter param : parameters) {
            copy.add(new Parameter(param.getName(), param.getType(), param.getValue(), param.getLabel()));
        }
        return copy;
    }
}
