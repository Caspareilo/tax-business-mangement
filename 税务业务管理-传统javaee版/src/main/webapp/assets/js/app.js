(function () {
    "use strict";

    var toggle = document.getElementById("sidebarToggle");
    var backdrop = document.getElementById("sidebarBackdrop");

    function setSidebarOpen(open) {
        document.body.classList.toggle("sidebar-open", open);
        if (toggle) {
            toggle.setAttribute("aria-expanded", open ? "true" : "false");
        }
    }

    if (toggle) {
        toggle.addEventListener("click", function () {
            setSidebarOpen(!document.body.classList.contains("sidebar-open"));
        });
    }

    if (backdrop) {
        backdrop.addEventListener("click", function () {
            setSidebarOpen(false);
        });
    }

    document.querySelectorAll(".app-nav-link").forEach(function (link) {
        link.addEventListener("click", function () {
            if (window.matchMedia("(max-width: 860px)").matches) {
                setSidebarOpen(false);
            }
        });
    });

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && document.body.classList.contains("sidebar-open")) {
            setSidebarOpen(false);
            if (toggle) {
                toggle.focus();
            }
        }
    });

    document.querySelectorAll("[data-nav-group]").forEach(function (groupToggle) {
        var panelId = groupToggle.getAttribute("aria-controls");
        var panel = panelId ? document.getElementById(panelId) : null;
        if (!panel) {
            return;
        }
        groupToggle.addEventListener("click", function () {
            var expanded = groupToggle.getAttribute("aria-expanded") === "true";
            groupToggle.setAttribute("aria-expanded", expanded ? "false" : "true");
            panel.hidden = expanded;
        });
    });

    document.querySelectorAll("form[data-confirm]").forEach(function (form) {
        form.addEventListener("submit", function (event) {
            var message = form.getAttribute("data-confirm");
            if (message && !window.confirm(message)) {
                event.preventDefault();
            }
        });
    });

    document.querySelectorAll("[data-confirm-button]").forEach(function (button) {
        button.addEventListener("click", function (event) {
            var message = button.getAttribute("data-confirm-button");
            if (message && !window.confirm(message)) {
                event.preventDefault();
            }
        });
    });

    document.querySelectorAll("select[data-reload-on-change]").forEach(function (select) {
        select.addEventListener("change", function () {
            var base = select.getAttribute("data-reload-on-change");
            if (base && select.value) {
                window.location.href = base + "?typeId=" + encodeURIComponent(select.value);
            }
        });
    });

    var fieldRows = document.getElementById("fieldRows");
    var addField = document.getElementById("addFieldBtn");
    var rowTemplate = document.getElementById("fieldRowTemplate");

    if (fieldRows && addField && rowTemplate) {
        addField.addEventListener("click", function () {
            var fragment = rowTemplate.content.cloneNode(true);
            fieldRows.appendChild(fragment);
            updateRowNumbers();
        });
        fieldRows.addEventListener("click", function (event) {
            var removeButton = event.target.closest("[data-remove-field]");
            if (removeButton) {
                var rows = fieldRows.querySelectorAll(".field-row");
                if (rows.length <= 1) {
                    window.alert("至少保留一个字段。");
                    return;
                }
                removeButton.closest(".field-row").remove();
                updateRowNumbers();
            }
        });
        fieldRows.addEventListener("change", function (event) {
            if (event.target.matches(".js-field-type")) {
                applyFieldTypeVisibility(event.target.closest(".field-row"));
            }
        });
        fieldRows.querySelectorAll(".field-row").forEach(applyFieldTypeVisibility);
    }

    function updateRowNumbers() {
        if (!fieldRows) {
            return;
        }
        fieldRows.querySelectorAll(".field-row").forEach(function (row, index) {
            var number = row.querySelector(".field-row-number");
            if (number) {
                number.textContent = String(index + 1);
            }
            var required = row.querySelector('input[name="fieldRequired"]');
            if (required) {
                required.value = String(index);
            }
        });
    }

    function applyFieldTypeVisibility(row) {
        if (!row) {
            return;
        }
        var select = row.querySelector(".js-field-type");
        if (!select) {
            return;
        }
        var value = select.value;
        row.querySelectorAll(".js-numeric-fields").forEach(function (element) {
            element.hidden = value !== "INTEGER" && value !== "AMOUNT";
        });
        row.querySelectorAll(".js-text-fields").forEach(function (element) {
            element.hidden = value !== "TEXT";
        });
        row.querySelectorAll(".js-options-fields").forEach(function (element) {
            element.hidden = value !== "RADIO" && value !== "MULTI";
        });
    }

    var flashAlerts = document.querySelectorAll(".app-flash");
    flashAlerts.forEach(function (alert) {
        window.setTimeout(function () {
            alert.classList.add("d-none");
        }, 8000);
    });
})();
