/*jslint browser: true, white: true, plusplus: true, regexp: true, indent: 4, maxerr: 50, es5: true */
/*jshint multistr: true, latedef: nofunc */
/*global jQuery, $, Swiper*/

var macSwiper,
    screenSliderTop,
    screenSliderThumbs,
    solverSlider,
    macNavigation = $('.controls-nav');

function stopMacVideos(){
    'use strict';
    $('.mac-wrap .swiper-slide video').each(function(){
        $(this).removeClass('is-playing')[0].pause();
    });
}

$(document).ready(function () {
    'use strict';
    macNavigation.on('click', 'mark', function(){
        macSwiper.slideTo( $(this).data('slide') );
        return false;
    });

    $('.mac-wrap').on('click', '.play-it', function(){
        stopMacVideos();
        $(this).prev().addClass('is-playing')[0].play();
        return false;
    });

    $('.we-solve').on('click', 'article > div', function(){
        var current = $(this),
            inx = current.index();
        if(!current.hasClass('is-opened')) {
            current.addClass('is-opened').siblings('div').removeClass('is-opened');
            $('.we-solve .problems-slider > div').slideUp(350);
            $('.we-solve .problems-slider > div:eq('+inx+')').slideDown({
                start: function () {
                    $(this).css({ display: 'flex' })
                }
            });
        } else {
            $('.we-solve article > div').removeClass('is-opened');
            $('.we-solve .problems-slider > div').slideUp(350);
        }
        return false;
    });


    $('.get-started-nav, .get-started').on('click', function() {
        $('#details-overlay').css('width', '100%');
    });

    $('.close-butt').on('click', function() {
        $('#details-overlay').css('width', '0%');
    });
});

$(window).on('load', function() {
    'use strict';
    macSwiper = new Swiper('.mac-wrap .swiper-container', {
        lazyLoadingInPrevNext   : true,
        preloadImages           : false,
        lazyLoading             : true,
        speed                   : 400,
        onSlideChangeStart      : function(swiper){
            $('mark', macNavigation).removeClass('is-current-slide');
            $('[data-slide="'+swiper.activeIndex+'"]', macNavigation).addClass('is-current-slide');
            stopMacVideos();
        }
    });

    screenSliderTop = new Swiper('.screen-slider-top', {
        nextButton              : '.screen-slider-top .swiper-button-next',
        prevButton              : '.screen-slider-top .swiper-button-prev',
        lazyLoadingInPrevNext   : true,
        preloadImages           : false,
        lazyLoading             : true
    });
    screenSliderThumbs = new Swiper('.screen-slider-thumbs', {
        slideToClickedSlide     : true,
        slidesPerView           : 'auto',
        centeredSlides          : true,
        spaceBetween            : 23
    });
    screenSliderTop.params.control = screenSliderThumbs;
    screenSliderThumbs.params.control = screenSliderTop;
});