$(function () {

    // ------------------------------------------------------- //
    // Tooltips init
    // ------------------------------------------------------ //    

    $('[data-toggle="tooltip"]').tooltip()        

    // ------------------------------------------------------- //
    // Universal Form Validation
    // ------------------------------------------------------ //

    $('.form-validate').each(function() {  
        $(this).validate({
            errorElement: "div",
            errorClass: 'is-invalid',
            validClass: 'is-valid',
            ignore: ':hidden:not(.summernote),.note-editable.card-block',
            errorPlacement: function (error, element) {
                // Add the `invalid-feedback` class to the error element
                error.addClass("invalid-feedback");
                //console.log(element);
                if (element.prop("type") === "checkbox") {
                    error.insertAfter(element.siblings("label"));
                } 
                else {
                    error.insertAfter(element);
                }
            }
        });
    });

    // ------------------------------------------------------- //
    // Material Inputs
    // ------------------------------------------------------ //

    var materialInputs = $('input.input-material');

    // activate labels for prefilled values
    materialInputs.filter(function() { return $(this).val() !== ""; }).siblings('.label-material').addClass('active');

    // move label on focus
    materialInputs.on('focus', function () {
        $(this).siblings('.label-material').addClass('active');
    });

    // remove/keep label on blur
    materialInputs.on('blur', function () {
        $(this).siblings('.label-material').removeClass('active');

        if ($(this).val() !== '') {
            $(this).siblings('.label-material').addClass('active');
        } else {
            $(this).siblings('.label-material').removeClass('active');
        }
    });

    // ------------------------------------------------------- //
    // Footer 
    // ------------------------------------------------------ //   

    var pageContent = $('.page-content');

    $(document).on('sidebarChanged', function () {
        adjustFooter();
    });

    $(window).on('resize', function(){
        adjustFooter();
    })

    function adjustFooter() {
        var footerBlockHeight = $('.footer__block').outerHeight();
        pageContent.css('padding-bottom', footerBlockHeight + 'px');
    }

    // ------------------------------------------------------- //
    // Adding fade effect to dropdowns
    // ------------------------------------------------------ //
    $('.dropdown').on('show.bs.dropdown', function () {
        $(this).find('.dropdown-menu').first().stop(true, true).fadeIn(100).addClass('active');
    });
    $('.dropdown').on('hide.bs.dropdown', function () {
        $(this).find('.dropdown-menu').first().stop(true, true).fadeOut(100).removeClass('active');
    });


    // ------------------------------------------------------- //
    // Search Popup
    // ------------------------------------------------------ //
    $('.search-open').on('click', function (e) {
        e.preventDefault();
        $('.search-panel').fadeIn(100);
    })
    $('.search-panel .close-btn').on('click', function () {
        $('.search-panel').fadeOut(100);
    });


    // ------------------------------------------------------- //
    // Sidebar Functionality
    // ------------------------------------------------------ //
    $('.sidebar-toggle').on('click', function () {
        $(this).toggleClass('active');

        $('#sidebar').toggleClass('shrinked');
        $('.page-content').toggleClass('active');
        $(document).trigger('sidebarChanged');

        if ($('.sidebar-toggle').hasClass('active')) {
            $('.navbar-brand .brand-sm').addClass('visible');
            $('.navbar-brand .brand-big').removeClass('visible');
            $(this).find('i').attr('class', 'fa fa-long-arrow-right');
        } else {
            $('.navbar-brand .brand-sm').removeClass('visible');
            $('.navbar-brand .brand-big').addClass('visible');
            $(this).find('i').attr('class', 'fa fa-long-arrow-left');
        }
    });


});


$(document).ready(function () {
    'use strict';
    Chart.defaults.global.defaultFontColor = '#75787c';
    loadBalanceData();
});



function loadBalanceData() {
    $.getJSON( "/balance_data", function( data ) {
           var data_labels = data["labels"];
           var data_values = data["values"];
           var min_value = data["min"];
           var max_value = data["max"];
           var spent_values = data["spent_values"];
           var income_values = data["income_values"];

            console.log(data_labels);
            console.log(data_values);
            // ------------------------------------------------------- //
            // Line Chart
            // ------------------------------------------------------ //
            var legendState = true;
            if ($(window).outerWidth() < 576) {
                legendState = false;
            }

            var LINECHART = $('#lineCahrt');
            var myLineChart = new Chart(LINECHART, {
                type: 'line',
                options: {
                    scales: {
                        xAxes: [{
                            display: true,
                            gridLines: {
                                display: false
                            }
                        }],
                        yAxes: [{
                            ticks: {
                                max: max_value,
                                min: min_value
                            },
                            display: true,
                            gridLines: {
                                display: false
                            }
                        }]
                    },
                    legend: {
                        display: legendState
                    }
                },
                data: {
                    labels: data_labels,
                    datasets: [
                        {
                            label: "Баланс",
                            fill: true,
                            lineTension: 0.2,
                            backgroundColor: "transparent",
                            borderColor: '#864DD9',
                            pointBorderColor: '#864DD9',
                            pointHoverBackgroundColor: '#864DD9',
                            borderCapStyle: 'butt',
                            borderDash: [],
                            borderDashOffset: 0.0,
                            borderJoinStyle: 'miter',
                            borderWidth: 2,
                            pointBackgroundColor: "#fff",
                            pointBorderWidth: 5,
                            pointHoverRadius: 5,
                            pointHoverBorderColor: "#fff",
                            pointHoverBorderWidth: 2,
                            pointRadius: 1,
                            pointHitRadius: 1,
                            data: data_values,
                            spanGaps: false
                        },
                        {
                            label: "Расходы",
                            fill: true,
                            lineTension: 0.2,
                            backgroundColor: "transparent",
                            borderColor: '#EF8C99',
                            pointBorderColor: '#EF8C99',
                            pointHoverBackgroundColor: '#EF8C99',
                            borderCapStyle: 'butt',
                            borderDash: [],
                            borderDashOffset: 0.0,
                            borderJoinStyle: 'miter',
                            borderWidth: 2,
                            pointBackgroundColor: "#fff",
                            pointBorderWidth: 5,
                            pointHoverRadius: 5,
                            pointHoverBorderColor: "#fff",
                            pointHoverBorderWidth: 2,
                            pointRadius: 1,
                            pointHitRadius: 1,
                            data: spent_values,
                            spanGaps: false
                        },
                        {
                            label: "Доходы",
                            fill: true,
                            lineTension: 0.2,
                            backgroundColor: "transparent",
                            borderColor: '#558000',
                            pointBorderColor: '#558000',
                            pointHoverBackgroundColor: '#558000',
                            borderCapStyle: 'butt',
                            borderDash: [],
                            borderDashOffset: 0.0,
                            borderJoinStyle: 'miter',
                            borderWidth: 2,
                            pointBackgroundColor: "#fff",
                            pointBorderWidth: 5,
                            pointHoverRadius: 5,
                            pointHoverBorderColor: "#fff",
                            pointHoverBorderWidth: 2,
                            pointRadius: 1,
                            pointHitRadius: 1,
                            data: income_values,
                            spanGaps: false
                        }


        //                {
        //                    label: "Page Views",
        //                    fill: true,
        //                    lineTension: 0.2,
        //                    backgroundColor: "transparent",
        //                    borderColor: "#EF8C99",
        //                    pointBorderColor: '#EF8C99',
        //                    pointHoverBackgroundColor: "#EF8C99",
        //                    borderCapStyle: 'butt',
        //                    borderDash: [],
        //                    borderDashOffset: 0.0,
        //                    borderJoinStyle: 'miter',
        //                    borderWidth: 2,
        //                    pointBackgroundColor: "#fff",
        //                    pointBorderWidth: 5,
        //                    pointHoverRadius: 5,
        //                    pointHoverBorderColor: "#fff",
        //                    pointHoverBorderWidth: 2,
        //                    pointRadius: 1,
        //                    pointHitRadius: 10,
        //                    data: [25, 17, 28, 25, 33, 27, 30, 33, 27],
        //                    spanGaps: false
        //                }
                    ]
                }
            });
    });
}