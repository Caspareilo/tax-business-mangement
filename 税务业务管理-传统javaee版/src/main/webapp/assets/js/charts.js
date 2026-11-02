(function () {
    "use strict";

    var dataNode = document.getElementById("chart-data");
    if (!dataNode || typeof Chart === "undefined") {
        return;
    }

    var data;
    try {
        data = JSON.parse(dataNode.textContent);
    } catch (error) {
        return;
    }

    var palette = ["#bfbfbf", "#1890ff", "#ff7875", "#52c41a", "#8c8c8c"];
    Chart.defaults.font.family = '-apple-system, BlinkMacSystemFont, "Segoe UI", "Microsoft YaHei", "PingFang SC", Arial, sans-serif';
    Chart.defaults.font.size = 12;
    Chart.defaults.color = "#8c8c8c";

    var numberFormat = new Intl.NumberFormat("zh-CN");
    var amountFormat = new Intl.NumberFormat("zh-CN", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });

    function compactAmount(value) {
        var number = Number(value);
        if (Math.abs(number) >= 10000) {
            return "¥" + (number / 10000).toFixed(number >= 100000 ? 0 : 1) + "万";
        }
        return "¥" + numberFormat.format(number);
    }

    function sparklineOptions() {
        return {
            responsive: true,
            maintainAspectRatio: false,
            animation: false,
            events: [],
            plugins: {
                legend: {display: false},
                tooltip: {enabled: false}
            },
            scales: {
                x: {display: false},
                y: {display: false, beginAtZero: true}
            },
            elements: {
                line: {borderWidth: 1.5, tension: 0.36},
                point: {radius: 0}
            }
        };
    }

    var metricCountCanvas = document.getElementById("metricCountChart");
    if (metricCountCanvas && data.trend && data.trend.counts) {
        new Chart(metricCountCanvas, {
            type: "line",
            data: {
                labels: data.trend.labels,
                datasets: [{
                    data: data.trend.counts,
                    borderColor: "#69b1ff",
                    backgroundColor: "rgba(24, 144, 255, 0.12)",
                    fill: true
                }]
            },
            options: sparklineOptions()
        });
    }

    var metricAmountCanvas = document.getElementById("metricAmountChart");
    if (metricAmountCanvas && data.trend && data.trend.amounts) {
        new Chart(metricAmountCanvas, {
            type: "line",
            data: {
                labels: data.trend.labels,
                datasets: [{
                    data: data.trend.amounts,
                    borderColor: "#73d13d",
                    backgroundColor: "rgba(82, 196, 26, 0.10)",
                    fill: true
                }]
            },
            options: sparklineOptions()
        });
    }

    var trendCanvas = document.getElementById("trendChart");
    if (trendCanvas && data.trend) {
        var trendChart = new Chart(trendCanvas, {
            data: {
                labels: data.trend.labels,
                datasets: [
                    {
                        type: "bar",
                        label: "申报数量",
                        data: data.trend.counts || [],
                        backgroundColor: "#4da3ff",
                        hoverBackgroundColor: "#1890ff",
                        borderRadius: 5,
                        borderSkipped: false,
                        maxBarThickness: 42,
                        order: 2
                    },
                    {
                        type: "line",
                        label: "申报金额",
                        data: data.trend.amounts || [],
                        borderColor: "#52c41a",
                        backgroundColor: "rgba(82, 196, 26, 0.10)",
                        fill: true,
                        borderWidth: 2,
                        pointRadius: 2.5,
                        pointHoverRadius: 4,
                        pointBackgroundColor: "#52c41a",
                        pointBorderColor: "#ffffff",
                        pointBorderWidth: 1,
                        tension: 0.28,
                        hidden: true,
                        order: 1
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: {mode: "index", intersect: false},
                plugins: {
                    legend: {display: false},
                    tooltip: {
                        backgroundColor: "rgba(38, 38, 38, 0.92)",
                        padding: 10,
                        displayColors: true,
                        callbacks: {
                            label: function (context) {
                                if (context.datasetIndex === 1) {
                                    return "申报金额：¥" + amountFormat.format(context.parsed.y);
                                }
                                return "申报数量：" + numberFormat.format(context.parsed.y) + " 笔";
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        grid: {display: false},
                        border: {display: false},
                        ticks: {color: "#8c8c8c", maxRotation: 0}
                    },
                    y: {
                        beginAtZero: true,
                        grid: {color: "#f0f0f0", drawTicks: false},
                        border: {display: false},
                        ticks: {
                            precision: 0,
                            padding: 8,
                            callback: function (value) {
                                return numberFormat.format(value);
                            }
                        }
                    }
                }
            }
        });

        var activeMetric = "counts";

        function selectTrendMetric(metric) {
            if (metric === activeMetric
                    || (metric === "amounts" && !data.trend.amounts)) {
                return;
            }
            activeMetric = metric;
            trendChart.data.datasets[0].hidden = metric !== "counts";
            trendChart.data.datasets[1].hidden = metric !== "amounts";
            trendChart.options.scales.y.ticks.callback = metric === "amounts"
                ? function (value) { return compactAmount(value); }
                : function (value) { return numberFormat.format(value); };
            trendChart.options.scales.y.ticks.precision = metric === "amounts" ? undefined : 0;
            trendChart.update();

            document.querySelectorAll("[data-trend-metric]").forEach(function (tab) {
                var selected = tab.getAttribute("data-trend-metric") === metric;
                tab.classList.toggle("active", selected);
                tab.setAttribute("aria-selected", selected ? "true" : "false");
            });
        }

        document.querySelectorAll("[data-trend-metric]").forEach(function (tab) {
            tab.addEventListener("click", function () {
                selectTrendMetric(tab.getAttribute("data-trend-metric"));
            });
            tab.addEventListener("keydown", function (event) {
                if (event.key === "ArrowLeft" || event.key === "ArrowRight") {
                    event.preventDefault();
                    selectTrendMetric(tab.getAttribute("data-trend-metric") === "counts"
                        ? "amounts" : "counts");
                    var next = document.querySelector("[data-trend-metric].active");
                    if (next) {
                        next.focus();
                    }
                }
            });
        });
    }

    var statusCanvas = document.getElementById("statusChart");
    if (statusCanvas && data.status) {
        new Chart(statusCanvas, {
            type: "doughnut",
            data: {
                labels: data.status.labels,
                datasets: [{
                    data: data.status.counts,
                    backgroundColor: palette,
                    borderColor: "#ffffff",
                    borderWidth: 2,
                    hoverOffset: 3
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: "66%",
                plugins: {
                    legend: {display: false},
                    tooltip: {
                        callbacks: {
                            label: function (context) {
                                return context.label + "：" + numberFormat.format(context.parsed) + " 笔";
                            }
                        }
                    }
                }
            }
        });
    }

    var typeCanvas = document.getElementById("typeChart");
    if (typeCanvas && data.types) {
        new Chart(typeCanvas, {
            type: "bar",
            data: {
                labels: data.types.labels,
                datasets: [{
                    label: "模拟金额（元）",
                    data: data.types.amounts,
                    backgroundColor: "#69b1ff",
                    hoverBackgroundColor: "#1890ff",
                    borderRadius: 3,
                    barThickness: 18
                }]
            },
            options: {
                indexAxis: "y",
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {display: false},
                    tooltip: {
                        callbacks: {
                            label: function (context) {
                                return "模拟金额：¥" + amountFormat.format(context.parsed.x);
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        beginAtZero: true,
                        grid: {color: "#f0f0f0"},
                        border: {display: false},
                        ticks: {callback: function (value) { return compactAmount(value); }}
                    },
                    y: {grid: {display: false}, border: {display: false}}
                }
            }
        });
    }

    var taxpayerCanvas = document.getElementById("taxpayerTypeChart");
    if (taxpayerCanvas && data.taxpayerTypes) {
        new Chart(taxpayerCanvas, {
            type: "bar",
            data: {
                labels: data.taxpayerTypes.labels,
                datasets: [{
                    label: "模拟金额（元）",
                    data: data.taxpayerTypes.amounts,
                    backgroundColor: "#95de64",
                    hoverBackgroundColor: "#52c41a",
                    borderRadius: 3,
                    barThickness: 22
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {display: false},
                    tooltip: {
                        callbacks: {
                            label: function (context) {
                                return "模拟金额：¥" + amountFormat.format(context.parsed.y);
                            }
                        }
                    }
                },
                scales: {
                    x: {grid: {display: false}, border: {display: false}},
                    y: {
                        beginAtZero: true,
                        grid: {color: "#f0f0f0"},
                        border: {display: false},
                        ticks: {callback: function (value) { return compactAmount(value); }}
                    }
                }
            }
        });
    }
})();
