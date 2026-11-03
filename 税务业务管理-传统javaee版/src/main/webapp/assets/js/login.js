(function () {
    "use strict";

    var tabs = Array.prototype.slice.call(document.querySelectorAll("[data-login-tab]"));
    var accountPanel = document.getElementById("accountPanel");
    var registerPanel = document.getElementById("registerPanel");
    var rolePanel = document.getElementById("rolePanel");
    var loginForm = document.getElementById("loginForm");
    var usernameInput = document.getElementById("username");
    var passwordInput = document.getElementById("password");
    var registerUsernameInput = document.getElementById("registerUsername");
    var registerPasswordInput = document.getElementById("registerPassword");
    var registerConfirmPasswordInput = document.getElementById("registerConfirmPassword");

    document.querySelectorAll("[data-password-toggle]").forEach(function (passwordToggle) {
        var targetId = passwordToggle.getAttribute("data-password-target");
        var targetInput = targetId ? document.getElementById(targetId) : passwordInput;
        if (!targetInput) {
            return;
        }
        passwordToggle.addEventListener("click", function () {
            var revealing = targetInput.type === "password";
            targetInput.type = revealing ? "text" : "password";
            passwordToggle.setAttribute("aria-pressed", revealing ? "true" : "false");
            passwordToggle.setAttribute("aria-label", revealing ? "隐藏密码" : "显示密码");
            passwordToggle.classList.toggle("is-visible", revealing);
            targetInput.focus();
        });
    });

    function activateTab(name, moveFocus) {
        tabs.forEach(function (tab) {
            var active = tab.getAttribute("data-login-tab") === name;
            tab.classList.toggle("active", active);
            tab.setAttribute("aria-selected", active ? "true" : "false");
            tab.setAttribute("tabindex", active ? "0" : "-1");
            if (active && moveFocus) {
                tab.focus();
            }
        });
        if (accountPanel) {
            accountPanel.hidden = name !== "account";
        }
        if (registerPanel) {
            registerPanel.hidden = name !== "register";
        }
        if (rolePanel) {
            rolePanel.hidden = name !== "role";
        }
        if (name === "account" && usernameInput) {
            window.setTimeout(function () {
                usernameInput.focus();
            }, 0);
        } else if (name === "register" && registerUsernameInput) {
            window.setTimeout(function () {
                registerUsernameInput.focus();
            }, 0);
        }
    }

    tabs.forEach(function (tab, index) {
        tab.addEventListener("click", function () {
            activateTab(tab.getAttribute("data-login-tab"), false);
        });
        tab.addEventListener("keydown", function (event) {
            if (event.key !== "ArrowLeft" && event.key !== "ArrowRight") {
                return;
            }
            event.preventDefault();
            var direction = event.key === "ArrowRight" ? 1 : -1;
            var nextIndex = (index + direction + tabs.length) % tabs.length;
            activateTab(tabs[nextIndex].getAttribute("data-login-tab"), true);
        });
    });

    document.querySelectorAll("[data-quick-login]").forEach(function (button) {
        button.addEventListener("click", function () {
            if (!loginForm || !usernameInput || !passwordInput) {
                return;
            }
            usernameInput.value = button.getAttribute("data-username") || "";
            passwordInput.value = button.getAttribute("data-password") || "";

            document.querySelectorAll("[data-quick-login]").forEach(function (item) {
                item.disabled = true;
                item.setAttribute("aria-busy", "true");
            });
            document.body.classList.add("quick-login-pending");

            if (typeof loginForm.requestSubmit === "function") {
                loginForm.requestSubmit();
            } else {
                loginForm.submit();
            }
        });
    });

    var registerForm = document.getElementById("registerForm");
    if (registerForm && registerPasswordInput && registerConfirmPasswordInput) {
        registerForm.addEventListener("submit", function (event) {
            if (registerPasswordInput.value !== registerConfirmPasswordInput.value) {
                event.preventDefault();
                registerConfirmPasswordInput.setCustomValidity("两次输入的密码不一致");
                registerConfirmPasswordInput.reportValidity();
                registerConfirmPasswordInput.focus();
                return;
            }
            registerConfirmPasswordInput.setCustomValidity("");
        });
        registerConfirmPasswordInput.addEventListener("input", function () {
            registerConfirmPasswordInput.setCustomValidity("");
        });
    }

    activateTab(document.body.getAttribute("data-initial-login-tab") || "account", false);
})();
