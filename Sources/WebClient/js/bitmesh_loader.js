$.getScript('./js/jquery.qrcode.js', function()
{
   $.getScript('./js/qrcode.js', function()
   {
      $.getScript('./js/Long.min.js', function()
      {
         $.getScript('./js/ByteBufferAB.min.js', function()
         {
            $.getScript('./js/ProtoBuf.js', function()
            {
               $.getScript('./js/bitcore.js', function()
               {
                  $.getScript('./js/bitcore-payment-protocol.js', function()
                  {
                     $.getScript('./js/bitcore-channel.js', function()
                     {
                        $.getScript('./js/bitmesh.js');
                     });
                  });
               });
            });
         });
      });
   });
});


