<%--
  Created by IntelliJ IDEA.
  User: andrew
  Date: 6/9/15
  Time: 9:58 PM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE HTML>
<html>
<head>
    <title>BitMesh</title>
    <link rel="icon" href="https://www.bitmesh.network/favicon.png" type="image/png">
    <meta property="og:image" content="images/bitmesh_wireless.png" />
    <meta property="og:title" content="BitMesh" />
    <meta property="og:description" content="BitMesh is a decentralized platform that allows people to leverage existing devices to share their internet connection with peers in exchange for bitcoin." />
    <meta http-equiv="content-type" content="text/html; charset=utf-8" />
    <meta name="description" content="BitMesh is a decentralized platform that allows people to leverage existing devices to share their internet connection with peers in exchange for bitcoin." />
    <meta name="keywords" content="" />
    <!--[if lte IE 8]><script src="css/ie/html5shiv.js"></script><![endif]-->
    <script src="js/jquery.min.js"></script>
    <script src="js/skel.min.js"></script>
    <script src="js/init.js"></script>
    <script src="js/bitmesh.js"></script>
    <link rel="stylesheet" href="css/bitmesh.css" />
    <noscript>
        <link rel="stylesheet" href="css/skel.css" />
        <link rel="stylesheet" href="css/style.css" />
        <link rel="stylesheet" href="css/style-wide.css" />
    </noscript>
    <!--[if lte IE 8]><link rel="stylesheet" href="css/ie/v8.css" /><![endif]-->
</head>
<body>

<!-- Header -->
<div id="header">
    <span class="bitmesh icon"></span>
    <h1>Hi. I heard you have extra bandwidth.</h1>
    <p>Why not let people around you connect and help pay your bill?
    </p>
</div>

<!-- Main -->
<div id="main">

    <header class="major container 75%">
        <h2>Bitmesh will revolutionize the way you share and purchase internet.
        </h2>
    </header>
    <div class="box container 85%">
        <p>
            BitMesh is developing a platform to allow people to share their internet connection with peers in exchange for bitcoin while leveraging existing devices and internet architecture. Negotiation of the price of data takes place in a local "marketplace", achieving a fair price for internet without requirement for a contract or third party escrow. The end result is cheaper, more robust internet service and bitcoin in more people’s hands.
        </p>

    </div>
    <br>
    <div class="box container">
        <section>
            <header>
                <h3>Our Mission</h3>
            </header>
            <p>Provide a solution that is ubiquitous across all devices, platforms and locations. This includes a phased approach to integrating with handset devices, desktop systems and eventually networking hardware. The resulting product is decentralized with no necessity to communicate or interact with anyone to begin buying and selling.</p>
        </section>
        <section>
            <header>
                <h3>FAQ</h3>
            </header>
            <p><b>How far along are you?</b>
                <br>
            <blockquote>We have a working prototype. You can set up a BitMesh server on a raspberry pi and have multiple clients connect to it. It establishes micropayment channels (a bitcoin technology) between the server and client and uses a captive portal to titrate internet time given to the user based on how much the user has paid so far. The user simply sets a policy and goes.</blockquote></p>
        </section>
        <section>
            <p><b>Why did you pick this idea to work on?</b>

            <blockquote>
                We set up a meshnet in our office one weekend. We were very excited about it and were plotting how to get downtown Santa Cruz on a meshnet, but were stumped on how to incentivize it. Coupons, bulletin boards, community chat, nothing seemed very compelling. We became  more interested in bitcoin and realized that the micropayment channel technology could allow people to pay only for the internet they use at very high resolution. I'd worked on several bitcoin projects by that time and Andrew has a strong networking background.<br><br>

                People use the internet all the time. They usually pay for it in big globs, and they can't resell it. Neighbors could save money by reselling their internet to each other. Coffeeshops could have a smoother on-ramp to their captive portals. We've lived through and are burned out on the contract based model for buying internet.
            </blockquote>
            </p>
        </section>
    </div>

    <div class="box container">
        <section>
            <header>
                <h3>Videos</h3>
            </header>
                <div id="video1_container" class="video-container">
                    <iframe src="https://www.youtube.com/embed/Euwb0_0wZPA" frameborder="0" allowfullscreen></iframe>
                </div>
        </section>
        <section>
                <div id="video2_container" class="video-container">
                    <iframe src="https://www.youtube.com/embed/yOb05a0yt1Y" frameborder="0" allowfullscreen></iframe>
                </div>
        </section>
    </div>

    <div class="box container" style="text-align:center;">
        <section>
            <header>
                <h3>Press</h3>
            </header>
            <p>
                <a href="https://a16z.com/2015/07/27/native-bitcoin-apps-open-source-developer-hearn/">On Native Bitcoin Apps, Open Source Communities, and What Happened Before Satoshi Nakamoto Vanished</a>, a16z 7/27/2015
                <br>
                <a href="http://www.verizonventures.com/blog/2015/06/startups-adopt-blockchain-to-disrupt-big-industry/">Startups Adopt Blockchain to Disrupt Big Industry</a>, Verizon Ventures 6/9/2015
                <br>
                <a href="http://coincenter.org/2015/06/what-are-micropayments-and-how-does-bitcoin-enable-them/">What are Micropayments and How does Bitcoin Enable Them?</a>, Coin Center 6/3/2015
                <br>
                <a href="http://bravenewcoin.com/news/10-bitcoin-industry-sectors-providing-killer-apps">10 Bitcoin Industry Sectors Providing Killer Apps</a>, Brave New Coin 5/28/2015
                <br>
                <a href="http://cointelegraph.com/news/113919/bandwidth-for-bitcoin-bitmesh-displays-working-prototype">Bandwidth for Bitcoin: BitMesh Displays Working Prototype</a>, Coin Telegraph 4/9/2015
                <br>
                <a href="http://bravenewcoin.com/news/bandwidth-for-bitcoin/">Bandwidth For Bitcoin</a>, Brave New Coin 4/3/2015
                <br>
                <a href="http://themerkle.com/news/interview-with-chris-smith-of-bitmesh/">Interview With Chris Smith Of BitMesh</a>, The Merkle 4/1/2015
                <br>
                <a href="http://www.coinbuzz.com/2015/03/30/bitmesh-to-launch-demo-soon/">BitMesh to Launch Demo Soon</a>, Coin Buzz 3/30/2015
                <br>
                <a href="http://themerkle.com/news/bitmesh-revolutionizing-internet-access-with-bitcoin/">BitMesh: Revolutionizing Internet Access With Bitcoin</a>, The Merkle 3/29/2015
                <br>
                <a href="http://cointelegraph.com/news/113474/why-bitmesh-could-become-the-uber-of-isps">Why BitMesh Could Become the Uber of ISPs</a>, Coin Telegraph 2/11/2015
                <br>
                <a href="http://siliconangle.com/blog/2015/02/03/got-extra-bandwidth-share-it-and-be-rewarded-with-bitcoin/">Got extra bandwidth? Share it for Bitcoin rewards</a>, Silicon Angle 2/3/2015
            </p>
        </section>
    </div>

    <div class="box container" style="text-align:center;">
        <!-- Begin MailChimp Signup Form -->
        <div id="mc_embed_signup">
            <form action="//network.us10.list-manage.com/subscribe/post?u=3dd8b66ba2ed0362c6cf5547c&amp;id=7d2ea55233" method="post" id="mc-embedded-sub
				scribe-form" name="mc-embedded-subscribe-form" class="validate" target="_blank" novalidate>
                <div id="mc_embed_signup_scroll">
                    <header>
                        <h2>Interested in learning more about BitMesh?</h2>
                    </header>


                    <div class="mc-field-group">
                        <label for="mce-EMAIL">Email Address  <span class="asterisk">*</span>
                        </label>
                        <input type="email" value="" name="EMAIL" class="required email" id="mce-EMAIL">
                    </div>
                    <div id="mce-responses" class="clear">
                        <div class="response" id="mce-error-response" style="display:none"></div>
                        <div class="response" id="mce-success-response" style="display:none"></div>
                    </div>    <!-- real people should not fill this in and expect good things - do not remove this or risk form bot signups-->
                    <div style="position: absolute; left: -5000px;"><input type="text" name="b_3dd8b66ba2ed0362c6cf5547c_7d2ea55233" tabindex="-1" value=""></div>
                    <div class="clear"><br><input type="submit" value="Subscribe" name="subscribe" id="mc-embedded-subscribe" class="button"></div>
                </div>
            </form>
            <h3>OR</h3>
            <a href="https://twitter.com/BitMeshNetwork" class="twitter-follow-button" data-show-count="false" data-size="large">Follow @BitMeshNetwork</a>
            <script>!function(d,s,id){var js,fjs=d.getElementsByTagName(s)[0],p=/^http:/.test(d.location)?'http':'https';if(!d.getElementById(id)){js=d.createElement(s);js.id=id;js.src=p+'://platform.twitter.com/widgets.js';fjs.parentNode.insertBefore(js,fjs);}}(document, 'script', 'twitter-wjs');</script>
        </div>

        <!--End mc_embed_signup-->

    </div>


    <div class="box container">
        <header>
            <h2>Our Crew</h2>
        </header>
        <section>
            <header>
                <h3>Andrew Donley</h3>
                <p>Founder - CEO</p>
            </header>
            <a href="https://www.linkedin.com/in/adonley">
                <div class="circle" style='background-image: url(./images/headshot-cropped.png)'></div>
            </a>
            <br/>
            <p>Andrew Donley builds custom front-end applications and embedded systems for enterprise clients in the software and hardware industry. His work is implemented across organizations ranging from start-ups to marquee technology firms including LG, Samsung, and Shop.com. Andrew specializes in programming with C, Android, Java, Bash, and JavaScript. He has a patent-pending for a compression algorithm that adheres to the gzip file format and compresses/decompresses 4-8x faster than standard gzip in most circumstances. The resulting files were decompress-able by any standard zlib - gzip decompression utility.

                Andrew holds a Bachelor of Science in Mathematics from the University of San Diego.</p>
            <ul class="icons center">
                <li><a href="https://www.linkedin.com/in/adonley" class="icon fa-linkedin"><span class="label">Linkedin</span></a></li>
                <li><a href="mailto:andrew@bitmesh.network" class="icon fa-send"><span class="label">Email</span></a></li>
            </ul>
            <!--<a href="mailto:andrew@bitmesh.network">andrew@bitmesh.network</a> PGP: <a href="./andrewatbitmesh.asc">Pub Key</a> -->
        </section>
        <hr />
        <section>
            <header>
                <h3>Chris Lunoe</h3>
                <p>Co-Founder - COO</p>
            </header>
            <a href="https://www.linkedin.com/in/christianlunoe">
                <div class="circle" style='background-image: url(./images/headshot_lunoe.png)'></div>
            </a>
            <br/>
            <p>Christian Lunoe, the Payments Practice Leader at comScore, Inc., advises blue-chip financial institutions on emerging digital payment trends to clients representing almost 90% of all digital transactions. His work in digital payments include publishing various white papers such as “The Digital Wallet Roadmap” and “State of E-Commerce and Digital Payments in India” and contributes to industry publications including “Retail TouchPoints”, “CRM Magazine”, “<a href="http://cardnotpresent.com/">CardNotPresent.com</a>” and the comScore blog. His thought leadership pieces are often featured at industry conferences including “Money 2020”, CyberSource’s “Payment Management Summit” and “Latin America Travel Payments Conference”, as well as Fiserv's “Forum”.
            </p>
            <p>Christian is a graduate of the Wharton School of the University of Pennsylvania with a Bachelor of Science in Economics specializing in entrepreneurial-, operations- and information-management.</p>
            <ul class="icons center">
                <li><a href="https://www.linkedin.com/in/clunoe" class="icon fa-linkedin"><span class="label">Linkedin</span></a></li>
                <li><a href="mailto:christian@bitmesh.network" class="icon fa-send"><span class="label">Email</span></a></li>
            </ul>
        </section>
        <hr />
        <section>
            <header>
                <h3>Chris Smith</h3>
                <p>Technical Advisor</p>
            </header>
            <a href="http://www.linkedin.com/pub/christopher-smith/52/963/565">
                <div class="circle" style='background-image: url(./images/headshot_smith.jpg)'></div>
            </a>
            <br/>
            <p>Chris has degrees in Computer Science and Mathematics from University of Miami, plays the piano and loves bitcoin. He worked as a developer and researcher at an R&amp;D consultancy where he met Andrew and worked on parallel computing, error-correcting codes, and compression algorithms. He has co-founded 2 other bitcoin companies as well as an educational video game company. Chris spends his free time making <a href="https://www.soundcloud.com/ganesha1024">music</a>, learning new human and computer languages and thinking about math and brains. Chris is excited to work on BitMesh.</p>
            <ul class="icons center">
                <li><a href="https://www.linkedin.com/pub/christopher-smith/52/963/565" class="icon fa-linkedin"><span class="label">Linkedin</span></a></li>
                <li><a href="mailto:christopher@bitmesh.network" class="icon fa-send"><span class="label">Email</span></a></li>
            </ul>
        </section>
    </div>


    <!-- <footer class="major container 75%">
        <h3>Get shady with science</h3>
        <p>Vitae natoque dictum etiam semper magnis enim feugiat amet curabitur tempor orci penatibus. Tellus erat mauris ipsum fermentum etiam vivamus.</p>
        <ul class="actions">
            <li><a href="#" class="button">Join our crew</a></li>
        </ul>
    </footer> -->

</div>




<!-- Footer -->
<div id="footer">


    <div class="container 75%">


        <br>

        <h2 class="major last">Questions or comments?</h2>

        <p>Email:
            <br>
            <a href="mailto:info@bitmesh.network">info@bitmesh.network</a>
            <br>
            <a href="mailto:investment@bitmesh.network">investment@bitmesh.network</a>
        </p>

        <ul class="icons">
            <li><a href="https://twitter.com/BitMeshNetwork" class="icon fa-twitter"><span class="label">Twitter</span></a></li>
            <li><a href="https://angel.co/bitmesh" class="icon fa-angellist"><span class="label">Angel List</span></a></li>
        </ul>


        <ul class="copyright">
            <li>&copy;2015 BitMesh Network, Inc. All rights reserved.</li><li>Design: <a href="http://html5up.net">HTML5 UP</a></li>
        </ul>
        <p>More later ;)</p>

    </div>
</div>
<script>
    (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
                (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
            m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
    })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

    ga('create', 'UA-59203527-1', 'auto');
    ga('send', 'pageview');

</script>

</body>
</html>
